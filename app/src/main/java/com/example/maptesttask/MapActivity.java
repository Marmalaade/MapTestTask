package com.example.maptesttask;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MapActivity extends AppCompatActivity implements MapEventsReceiver {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapView mapView;
    private int currentMarkerIndex = 0;
    private final ArrayList<OverlayItem> markerList = new ArrayList<>();
    private ItemizedIconOverlay<OverlayItem> itemizedIconOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        initializeMap();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        if (itemizedIconOverlay == null) {
            ArrayList<OverlayItem> items = new ArrayList<>();
            itemizedIconOverlay = new ItemizedIconOverlay<>(getApplicationContext(), items, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(int index, OverlayItem item) {
                    showDialogFragment();
                    return true;
                }

                @Override
                public boolean onItemLongPress(int index, OverlayItem item) {
                    return false;
                }
            });
            mapView.getOverlayManager().add(itemizedIconOverlay);
        }

        if (markerList.size() >= 3) {
            markerList.clear();
            itemizedIconOverlay.removeAllItems();
        }

        OverlayItem marker = new OverlayItem("Marker", "osmMapMarker", p);
        Drawable markerDrawable = ContextCompat.getDrawable(this, R.drawable.ic_map_marker);
        marker.setMarker(markerDrawable);
        itemizedIconOverlay.addItem(marker);
        mapView.invalidate();
        markerList.add(marker);

        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    public void goToTheNextMarker(View view) {
        if (markerList.isEmpty()) {
            return;
        }

        currentMarkerIndex++;
        if (currentMarkerIndex >= markerList.size()) {
            currentMarkerIndex = 0;
        }

        OverlayItem nextMarker = markerList.get(currentMarkerIndex);
        GeoPoint nextMarkerPoint = (GeoPoint) nextMarker.getPoint();

        mapView.getController().animateTo(nextMarkerPoint);
        long delayMillis = 1000;

        mapView.postDelayed(() -> {
            showCustomInfoWindow(nextMarker, nextMarkerPoint);
        }, delayMillis);
    }

    private void initializeMap() {
        Configuration.getInstance().load(getApplicationContext(), getPreferences(MODE_PRIVATE));
        setContentView(R.layout.activity_map);

        ImageView zoomInButton = findViewById(R.id.zoomIn);
        ImageView zoomOutButton = findViewById(R.id.zoomOut);
        ImageView currentPosition = findViewById(R.id.currentLocation);
        ImageView goToNextLocation = findViewById(R.id.nextMarker);


        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);
        mapView.getOverlayManager().add(mapEventsOverlay);

        zoomInButton.setOnClickListener(v -> mapView.getController().zoomIn());
        zoomOutButton.setOnClickListener(v -> mapView.getController().zoomOut());
        currentPosition.setOnClickListener(v -> getLocation());
        goToNextLocation.setOnClickListener(this::goToTheNextMarker);
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            GeoPoint currentLocation = new GeoPoint(latitude, longitude);
                            mapView.getController().setCenter(currentLocation);
                            mapView.getController().setZoom(18);

                            Marker marker = new Marker(mapView);
                            marker.setPosition(currentLocation);

                            Drawable markerDrawable = ContextCompat.getDrawable(this, R.drawable.ic_mylocation_55dp);
                            marker.setIcon(markerDrawable);
                            mapView.getOverlays().add(marker);
                            mapView.invalidate();
                        }
                    });
        }
    }

    private void showCustomInfoWindow(OverlayItem marker, GeoPoint p) {
        View customInfoWindowView = getLayoutInflater().inflate(R.layout.marker_info_window_layout, null);
        TextView currentTime = customInfoWindowView.findViewById(R.id.markerTime);
        TextView nameInfo = customInfoWindowView.findViewById(R.id.nameInfo);
        currentTime.setText("GPS," + getCurrentTime());
        nameInfo.setText("Александр");

        PopupWindow customInfoWindow = new PopupWindow(customInfoWindowView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        customInfoWindow.setTouchable(true);
        customInfoWindow.setFocusable(true);
        customInfoWindow.setOutsideTouchable(true);

        GeoPoint markerPosition = (GeoPoint) marker.getPoint();
        Point markerPoint = new Point();
        mapView.getProjection().toPixels(markerPosition, markerPoint);
        customInfoWindow.showAtLocation(mapView, Gravity.NO_GRAVITY, markerPoint.x, markerPoint.y);
    }

    private String getCurrentTime() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date currentTime = new Date();
        return dateFormat.format(currentTime);
    }

    private void showDialogFragment() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_dialog_layout);
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Disable location access", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
