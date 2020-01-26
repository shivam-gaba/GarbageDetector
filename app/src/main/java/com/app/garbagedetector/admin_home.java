package com.app.garbagedetector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dmax.dialog.SpotsDialog;

import static com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newCameraPosition;

public class admin_home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, PermissionsListener, AdapterView.OnItemSelectedListener {

    private MapView mapViewAdmin;
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private LocationComponent locationComponent;

    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference markerReference;

    Button btnAddSpot;
    private String garbageType;

    List<marker> markers = new ArrayList<marker>();
    ArrayList<Double> lat = new ArrayList<Double>();
    ArrayList<Double> lng = new ArrayList<Double>();
    ArrayList<String> type = new ArrayList<String>();

    Button btnCancel;
    LinearLayout removeMarkersCancelLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, "pk.eyJ1Ijoic2hpdjg5NjhzaCIsImEiOiJjazVpZmY1eWIwY3Z3M21udnUwMHo5dzhnIn0.pkfihC9BK2VQTZwy-DMJLw");
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_admin_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        mapViewAdmin = findViewById(R.id.mapViewAdmin);
        mapViewAdmin.onCreate(savedInstanceState);
        mapViewAdmin.getMapAsync(this);
        final AlertDialog alertDialog = new SpotsDialog(admin_home.this);
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

        btnCancel = findViewById(R.id.btnCancelRemove);
        removeMarkersCancelLayout = findViewById(R.id.removeMarkersCancelLayout);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeMarkersCancelLayout.setVisibility(View.GONE);
                mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        return false;
                    }
                });
            }
        });


        btnAddSpot = findViewById(R.id.btnAddSpot);
        btnAddSpot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddSpotDialog();
            }
        });
    }


    private void addMarkersFromDatabase() {

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        markerReference = database.getReferenceFromUrl("https://grabage-detector.firebaseio.com/Markers");
        markerReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot keyNode : dataSnapshot.getChildren()) {
                    marker m = keyNode.getValue(marker.class);
                    markers.add(m);
                }

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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(admin_home.this, "ERROR: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        garbageType = adapterView.getItemAtPosition(i).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void showAddSpotDialog() {
        final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(this);
        dialog.setTitle("Add Spot");

        LayoutInflater inflater = LayoutInflater.from(this);
        View add_spot_layout = inflater.inflate(R.layout.add_spot_layout, null);

        final MaterialEditText etLat = add_spot_layout.findViewById(R.id.etLat);
        final MaterialEditText etLng = add_spot_layout.findViewById(R.id.etLng);
        final Spinner spinner = add_spot_layout.findViewById(R.id.spinner);

        etLat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etLng.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);

        ArrayList<String> list = new ArrayList<String>();
        list.add("Low");
        list.add("Medium");
        list.add("High");

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(adapter);

        dialog.setView(add_spot_layout);

        dialog.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (TextUtils.isEmpty(etLat.getText().toString())) {
                    Toast.makeText(admin_home.this, "Enter all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(etLng.getText().toString())) {
                    Toast.makeText(admin_home.this, "Enter all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    MarkerOptions markerOptions = new MarkerOptions()
                            .title(spinner.getSelectedItem().toString())
                            .position(new LatLng(Double.parseDouble(etLat.getText().toString()), Double.parseDouble(etLng.getText().toString())));
                    mapboxMap.addMarker(markerOptions);

                    mapboxMap.animateCamera(newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(Double.parseDouble(etLat.getText().toString()), Double.parseDouble(etLng.getText().toString())))
                                    .zoom(14).build()), 4000);

                    dialogInterface.dismiss();
                    Toast.makeText(admin_home.this, "Spot Added", Toast.LENGTH_LONG).show();

                    marker m = new marker();
                    m.setLat(Double.parseDouble(etLat.getText().toString()));
                    m.setLng(Double.parseDouble(etLng.getText().toString()));
                    m.setType(spinner.getSelectedItem().toString());

                    markerReference
                            .child(Objects.requireNonNull(markerReference.push().getKey()))
                            .setValue(m)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(admin_home.this, "Marker added On database", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(admin_home.this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                } catch (Exception e) {
                    Toast.makeText(admin_home.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialog.show();
    }

    //Maps

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
        mapViewAdmin.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapViewAdmin.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapViewAdmin.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapViewAdmin.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapViewAdmin.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapViewAdmin.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapViewAdmin.onLowMemory();
    }


//Navigation


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
        getMenuInflater().inflate(R.menu.admin_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case (R.id.logOut):
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(admin_home.this, MainActivity.class));
                break;

/*
            case (R.id.delete_marker): {
                if (!mapboxMap.getMarkers().isEmpty()) {

                    Toast.makeText(this, "Select the spot you want to delete", Toast.LENGTH_LONG).show();

                    removeMarkersCancelLayout.setVisibility(View.VISIBLE);
                    final LatLngBounds bounds = new LatLngBounds.Builder().include(new LatLng(lat.get(0), lng.get(0))).include(new LatLng(lat.get(markers.size() - 1), lng.get(markers.size() - 1))).build();
                    mapboxMap.animateCamera(CameraUpdateFactory
                            .newLatLngBounds(bounds, 100), 4000);
                    mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {

                        @Override
                        public boolean onMarkerClick(@NonNull final Marker marker) {
                            try {
                                marker.remove();
                                Toast.makeText(admin_home.this, "Spot removed", Toast.LENGTH_SHORT).show();

                                DatabaseReference removeMarkerReference = FirebaseDatabase.getInstance().getReference();
                                Query removeMarkerQuery = removeMarkerReference.child("Markers").orderByChild("lat").equalTo(marker.getPosition().getLatitude());

                                removeMarkerQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot markerSnapshot : dataSnapshot.getChildren()) {
                                            markerSnapshot.getRef().removeValue();
                                            mapboxMap.clear();
                                            mapboxMap.removeAnnotations();

                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NotNull DatabaseError databaseError) {
                                        Toast.makeText(admin_home.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } catch(Exception e) {
                                Toast.makeText(admin_home.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        }
                    });
                } else {
                    Toast.makeText(this, "No spots found", Toast.LENGTH_SHORT).show();
                }
            }

            break;
            */
        }
        return true;
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
}