package com.app.garbagedetector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class user_home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, PermissionsListener {

    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private LocationComponent locationComponent;
    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;

    private LatLng startLatLng;
    private LatLng endLatLng;
    private double startLng, startLat, endLng, endLat;

    private static int markerNumber = 0;
    private int availableMarkers = 0;

    private Button btnCheckSpot, btnNext, btnPrevious, btnGetRoute, btnCancel, btnNavigate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, "pk.eyJ1Ijoic2hpdjg5NjhzaCIsImEiOiJjazVpZmY1eWIwY3Z3M21udnUwMHo5dzhnIn0.pkfihC9BK2VQTZwy-DMJLw");
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_user_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        final AlertDialog alertDialog = new SpotsDialog(user_home.this);
        alertDialog.show();

        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();

                    addMarkersFromDatabase();
                }
            }
        };
        handler.postDelayed(runnable, 3000);

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        btnCheckSpot = findViewById(R.id.btnCheckSpot);
        btnGetRoute = findViewById(R.id.btnGetRoute);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnCancel = findViewById(R.id.btnCancel);
        btnNavigate = findViewById(R.id.btnNavigate);

        btnCheckSpot.setVisibility(View.VISIBLE);
        btnGetRoute.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        btnPrevious.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnNavigate.setVisibility(View.GONE);

        btnCheckSpot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (markerNumber < availableMarkers) {

                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(mapboxMap.getMarkers().get(markerNumber).getPosition()))
                                    .zoom(14)
                                    .build()), 4000);

                    btnCheckSpot.setVisibility(View.GONE);
                    btnGetRoute.setVisibility(View.VISIBLE);
                    btnNext.setVisibility(View.VISIBLE);
                    btnPrevious.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(user_home.this, "No More Spots found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                markerNumber++;
                if (markerNumber < availableMarkers) {

                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(mapboxMap.getMarkers().get(markerNumber).getPosition()))
                                    .zoom(14)
                                    .build()), 4000);

                } else {
                    markerNumber--;
                    Toast.makeText(user_home.this, "No More Spots found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                markerNumber--;
                if (markerNumber >= 0) {

                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(mapboxMap.getMarkers().get(markerNumber).getPosition()))
                                    .zoom(14)
                                    .build()), 4000);

                } else {
                    markerNumber++;
                    Toast.makeText(user_home.this, "No Previous Spots found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnGetRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btnPrevious.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
                btnGetRoute.setVisibility(View.GONE);
                btnCancel.setVisibility(View.VISIBLE);
                btnNavigate.setVisibility(View.VISIBLE);

                startLatLng = new LatLng(locationComponent.getLastKnownLocation().getLatitude(), locationComponent.getLastKnownLocation().getLongitude());
                endLatLng = mapboxMap.getMarkers().get(markerNumber).getPosition();

                startLat = startLatLng.getLatitude();
                startLng = startLatLng.getLongitude();
                endLat = endLatLng.getLatitude();
                endLng = endLatLng.getLongitude();


                getRoute(Point.fromLngLat(startLng, startLat), Point.fromLngLat(endLng, endLat));

                LatLngBounds latLngBounds = new LatLngBounds.Builder()
                        .include(startLatLng)
                        .include(endLatLng)
                        .build();
                mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200), 5000);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnCancel.setVisibility(View.GONE);
                btnNavigate.setVisibility(View.GONE);
                btnPrevious.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.VISIBLE);
                btnGetRoute.setVisibility(View.VISIBLE);

                navigationMapRoute.removeRoute();

            }
        });

        btnNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                navigate();
            }
        });

    }

    private void navigate() {
        if (currentRoute != null) {
            NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                    .directionsRoute(currentRoute)
                    .shouldSimulateRoute(true)
                    .build();
// Call this method with Context from within an Activity
            NavigationLauncher.startNavigation(user_home.this, options);
        } else {
            Toast.makeText(this, "Loading Map. Please Click again !", Toast.LENGTH_SHORT).show();
        }
    }


    //       Navigation Menu

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Maps

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                enableLocationComponent(style);

            }
        });
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

// Get an instance of the component
            locationComponent = mapboxMap.getLocationComponent();

// Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void getRoute(Point startLatLng, Point endLatLng) {

        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(startLatLng)
                .destination(endLatLng)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null || response.body().routes().size() == 00) {
                            Toast.makeText(user_home.this, "No Routes found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        if (navigationMapRoute != null) {
                            navigationMapRoute.updateRouteArrowVisibilityTo(false);
                            navigationMapRoute.updateRouteArrowVisibilityTo(false);
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Toast.makeText(user_home.this, "Failed :" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                });
    }


    private void addMarkersFromDatabase() {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference markerReference = database.getReferenceFromUrl("https://grabage-detector.firebaseio.com/Markers");
        markerReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<marker> markers = new ArrayList<marker>();
                for (DataSnapshot keyNode : dataSnapshot.getChildren()) {
                    marker m = keyNode.getValue(marker.class);
                    markers.add(m);
                }

                ArrayList<Double> lat = new ArrayList<Double>();
                ArrayList<Double> lng = new ArrayList<Double>();
                ArrayList<String> type = new ArrayList<String>();

                for (int i = 0; i < markers.size(); i++) {
                    lat.add(markers.get(i).getLat());
                    lng.add(markers.get(i).getLng());
                    type.add(markers.get(i).getType());
                }


                for (int i = 0; i < markers.size(); i++) {

                    MarkerOptions markerOptions = new MarkerOptions()
                            .title(type.get(i))
                            .position(new LatLng(lat.get(i), lng.get(i)));
                    mapboxMap.addMarker(markerOptions);
                }
                availableMarkers=markers.size();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(user_home.this, "ERROR: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
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

    @Override
    protected void onStop() {
        super.onStop();
        markerNumber = 0;
        mapboxMap.clear();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        markerNumber = 0;
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
