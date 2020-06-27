package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.app.Activity
import androidx.room.Room
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import android.graphics.Color
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.style.layers.PropertyFactory

class MapPopupActivity : AppCompatActivity() {
    private var mapPoints: ArrayList<MeasurementMapPoint> = ArrayList()
    private lateinit var db: AppDatabase
    private var sessionID: String = ""
    private var mapView: MapView? = null
    private var exportedSessions: ArrayList<Session> = ArrayList()
    private lateinit var routeCoordinates: ArrayList<Point>

    companion object {
        const val SESSION_ID = "sessionID"
        const val EXPORTED_SESSIONS = "exportedSessions"

        fun newIntent(context: Context, session: Session, exportedSessions: ArrayList<Session>): Intent {
            val detailIntent = Intent(context, MapPopupActivity::class.java)
            detailIntent.putExtra(SESSION_ID, session.sessionID)
            detailIntent.putExtra(EXPORTED_SESSIONS, exportedSessions)
            return detailIntent
        }
    }

    internal inner class GetMapPointsFromDB : Runnable {
        override fun run() {
            val listSessionList = db.measurementPointDao().getRandomMapPointsBySessionID(sessionID)
            mapPoints = ArrayList(listSessionList)
        }
    }

    internal inner class DeleteSessionFromDB : Runnable {
        override fun run() {
            db.measurementPointDao().deleteBySessionID(sessionID)
        }
    }

    private fun getMapPoints() {
        val t = Thread(GetMapPointsFromDB())
        t.start()
        t.join()
    }

    private fun initRouteCoordinates() {
        // Create a list to store our line coordinates.
        routeCoordinates = ArrayList()
        for (m in mapPoints) {
            routeCoordinates.add(Point.fromLngLat(m.locLongitude, m.locLatitude))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_map_popup)

        sessionID = intent.extras.getString(SESSION_ID)
        exportedSessions = intent.extras.getParcelableArrayList<Session>(EXPORTED_SESSIONS)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "measurements"
        ).build()
        getMapPoints()

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {style ->
                initRouteCoordinates()
                val features = List<Feature>(routeCoordinates.size){ Feature.fromGeometry(
                    LineString.fromLngLats(routeCoordinates)
                )}

                style.addSource(
                    GeoJsonSource("line-source", FeatureCollection.fromFeatures(features))
                )
                style.addLayer(
                    LineLayer("linelayer", "line-source").withProperties(
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineWidth(5f),
                        PropertyFactory.lineColor(Color.parseColor("#e55e5e"))
                    )
                )
                val cameraPos = LatLng(routeCoordinates[0].latitude(), routeCoordinates[0].longitude())
                val position = CameraPosition.Builder()
                    .target(cameraPos)
                    .zoom(12.0)
                    .tilt(20.0)
                    .build()
                mapboxMap.cameraPosition = position
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, SecondActivity::class.java)
        intent.putParcelableArrayListExtra(EXPORTED_SESSIONS, exportedSessions)
        setResult(Activity.RESULT_OK, intent)
        super.onBackPressed()
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
        db.close()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
}
