package com.parseal.app.parsealtest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.content_actor_dialog_interface.view.*
import kotlin.math.roundToInt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val db = Firebase.firestore
    private lateinit var client: PlacesClient
    private var actors: List<Actor> = emptyList()
    private lateinit var startAdapter: PlacesAdapter
    private lateinit var endAdapter: PlacesAdapter
    private lateinit var registration: ListenerRegistration
    private val bounds: LatLngBounds = LatLngBounds.Builder()
        .include(LatLng(6.4011835, 3.3972372))
        .include(LatLng(6.526719399999999, 3.6722513))
        .build()

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        client = Places.createClient(applicationContext)
        getActors()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        startAdapter = PlacesAdapter(this, bounds, client)
        endAdapter = PlacesAdapter(this, bounds, client)
        fab.setOnClickListener {
            if (actors.size < 3) {
                val v = layoutInflater.inflate(R.layout.content_actor_dialog_interface, null)
                val dialog = AlertDialog.Builder(this, R.style.DialogTheme)
                    .setView(v)
                    .setCancelable(false)
                    .create()
                v.subhead.text = ("${actors.size + 1} of 3")
                v.start_address_edtv.setAdapter(startAdapter)
                v.end_address_edtv.setAdapter(endAdapter)
                v.cancel_btn.setOnClickListener {
                    dialog.dismiss()
                }
                v.submit_btn.setOnClickListener {
                    if (startAdapter.selectedPlace != null && endAdapter.selectedPlace != null) {
                        createActor(startAdapter.selectedPlace!!, endAdapter.selectedPlace!!)
                        dialog.dismiss()
                    } else {
                        val place = if (startAdapter.selectedPlace == null) {
                            v.start_address_layout.apply {
                                isErrorEnabled = true
                                error = ("Please select a start address")
                            }
                            v.start_address_edtv.requestFocus()
                            "Start Address"
                        } else {
                            v.end_address_layout.apply {
                                isErrorEnabled = true
                                error = ("Please select a start address")
                            }
                            v.end_address_edtv.requestFocus()
                            "End Address"
                        }
                        Toast.makeText(
                            this,
                            "Please select your $place to continue",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                if (!isFinishing)
                    dialog.show()
            }
        }
    }

    //Adding actor item to the backend
    private fun createActor(start: Place, end: Place) {
        val data = mapOf(
            "name" to "Actor ${actors.size + 1}",
            "start_location" to mapOf(
                "longitude" to (start.latLng?.longitude ?: 0.0),
                "latitude" to (start.latLng?.latitude ?: 0.0)
            ),
            "current_location" to mapOf(
                "longitude" to (start.latLng?.longitude ?: 0.0),
                "latitude" to (start.latLng?.latitude ?: 0.0)
            ),
            "start_address" to start.address,
            "end_location" to mapOf(
                "longitude" to (end.latLng?.longitude ?: 0.0),
                "latitude" to (end.latLng?.latitude ?: 0.0)
            ),
            "end_address" to end.address
        )
        db.collection("actors").add(data).addOnSuccessListener {
            Log.e("MapsActivity", "Document ID: ${it.id}")
            Toast.makeText(
                this,
                "Added Actor ${actors.size + 1} to db successfully",
                Toast.LENGTH_SHORT
            ).show()
        }.addOnFailureListener {
            it.printStackTrace()
            Toast.makeText(this, "Failed to add Actor ${actors.size + 1}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getActors() {
        registration = db.collection("actors").addSnapshotListener { querySnapshot, e ->
            if (e != null) {
                e.printStackTrace()
                return@addSnapshotListener
            }
            if (querySnapshot != null) {
                actors = try {
                    querySnapshot.toObjects(Actor::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
                if (actors.size >= 3)
                    fab.hide()
                else fab.show()
                updateMap(actors)
            }
        }
    }

    private fun updateMap(actors: List<Actor>) {
        if (::mMap.isInitialized) {
            actors.forEach {
                mMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(it.start_location["latitude"] ?: 0.0, it.start_location["longitude"] ?: 0.0))
                        .title(it.name)
                        .snippet(it.start_address)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                            when(actors.indexOf(it)) {
                                0 -> BitmapDescriptorFactory.HUE_GREEN
                                1 -> BitmapDescriptorFactory.HUE_CYAN
                                else -> BitmapDescriptorFactory.HUE_ORANGE
                            }
                        )))
                mMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(it.end_location["latitude"] ?: 0.0, it.end_location["longitude"] ?: 0.0))
                        .title(it.name)
                        .snippet(it.end_address)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                            when(actors.indexOf(it)) {
                                0 -> BitmapDescriptorFactory.HUE_GREEN
                                1 -> BitmapDescriptorFactory.HUE_CYAN
                                else -> BitmapDescriptorFactory.HUE_ORANGE
                            }
                        )))
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.apply {
            //isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
        }

        // Add a marker in Sydney and move the camera
        mMap.addMarker(MarkerOptions().position(bounds.center).title("Et-Osa Lagos"))
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
            bounds,
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels,
            (resources.displayMetrics.widthPixels * 0.05).roundToInt()
        ), object : GoogleMap.CancelableCallback {
            override fun onFinish() {
                mMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(bounds.center)
                            .zoom(10.5f)
                            .bearing(0f)
                            .tilt(50f)
                            .build()
                    )
                )
            }

            override fun onCancel() {

            }

        })
    }

    override fun onStop() {
        super.onStop()
        registration.remove()
    }


}
