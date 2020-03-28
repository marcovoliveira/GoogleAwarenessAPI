package pt.ipleiria.awareness2020pl1

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.fence.*
import com.google.android.gms.awareness.state.HeadphoneState
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.android.synthetic.main.activity_main.*
import pt.ipleiria.awareness2020pl1.Utils.checkFineLocationPermission
import java.sql.Timestamp


class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    val TAG_FENCES = "fences"
    val FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION"

    private lateinit var fenceReceiver: FenceReceiver
    private lateinit var myPendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById<TextView>(R.id.textView_main)

        setupFences()
    }

    fun onClick_button_snapshot(view: View) {
//        printHeadphoneState()
        printLocation()
//        printDetectedActivity()
        printNearbyPlaces()
    }

    fun onClick_button_register(view: View){
        val headphoneFence: AwarenessFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN)
        addFence("headphoneFence", headphoneFence)
        val walkingFence: AwarenessFence = DetectedActivityFence.during(DetectedActivityFence.WALKING)
        addFence("walkingFence", walkingFence)
        val walkingWithHeadphonesFence = AwarenessFence.and(
            walkingFence, headphoneFence
        )
        addFence("walkingWithHeadphonesFence", walkingWithHeadphonesFence)
        val nowMillis = System.currentTimeMillis()
        val oneMinuteMilis = 60L * 1000L
        val thirtySecondsMillis = 30L * 1000L
        val timeFence: AwarenessFence = TimeFence.inInterval(
            nowMillis + thirtySecondsMillis,  // starting in thirty seconds
            nowMillis + thirtySecondsMillis + oneMinuteMilis // lasts for one minute
        )

        addFence("timeFence", timeFence)
    }

    private fun setupFences() {
        val intent = Intent("FENCE_RECEIVER_ACTION")
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
        fenceReceiver = FenceReceiver()
        registerReceiver(fenceReceiver, IntentFilter("FENCE_RECEIVER_ACTION"))
    }


    fun onClick_button_remove(view: View?) {
        removeFences()
    }

    private fun removeFences() {
        Awareness.getFenceClient(this).updateFences(
            FenceUpdateRequest.Builder()
                .removeFence(myPendingIntent)
                .build()
        )
            .addOnSuccessListener {
                val timestamp = Timestamp(System.currentTimeMillis())
                val text = "[Fences @ $timestamp] Fences were successfully removed.".trimIndent()
                textView.text = text + "\n" + textView_main.text
            }
            .addOnFailureListener { e ->
                val timestamp = Timestamp(System.currentTimeMillis())
                val text = "[Fences @ $timestamp] Fences could not be removed: ${e.message}".trimIndent()
                textView.text = text + "\n" + textView.text
            }
    }

    fun onClick_button_query(view: View?) {
        queryFences()
    }

    protected fun queryFences() {
        Awareness.getFenceClient(this).queryFences(FenceQueryRequest.all())
            .addOnSuccessListener { fenceQueryResponse ->
                var fenceInfo = ""
                val fenceStateMap = fenceQueryResponse.fenceStateMap
                for (fenceKey in fenceStateMap.fenceKeys) {
                    val state = fenceStateMap.getFenceState(fenceKey).currentState
                    fenceInfo += "$fenceKey: ${if (state == FenceState.TRUE) "TRUE" else if (state == FenceState.FALSE) "FALSE" else "UNKNOWN"}".trimIndent()
                }
                val timestamp = Timestamp(System.currentTimeMillis())
                val text = "[Fences @ $timestamp]> Fences' states: ${if (fenceInfo == "") "No registered fences." else fenceInfo}".trimIndent()
                textView.text = text + "\n" + textView.text
            }
            .addOnFailureListener { e ->
                val timestamp = Timestamp(System.currentTimeMillis())
                val text = "[Fences @ $timestamp] Fences could not be queried: ${e.message}".trimIndent()
                textView.text = text + "\n" + textView.text
            }
    }

    private fun addFence(fenceKey: String, fence: AwarenessFence) {
        Awareness.getFenceClient(this).updateFences(
            FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntent)
                .build()
        )
            .addOnSuccessListener {
                val timestamp = Timestamp(System.currentTimeMillis())
                val text = "[Fences @ $timestamp] Fence $fenceKey was successfully registered.".trimIndent()
                textView.text = text + "\n" + textView.text
            }
            .addOnFailureListener { e ->
                val timestamp = Timestamp(System.currentTimeMillis())
                val text = "[Fences @ $timestamp] Fence ${fenceKey} could not be registered: ${e.message}".trimIndent()
                textView.text = text + "\n" + textView.text
            }
    }

    private fun printLocation() {
        Utils.checkFineLocationPermission(this)
        Awareness.getSnapshotClient(this).location
                .addOnSuccessListener { locationResponse ->
                    val location: Location = locationResponse.location
                    val timestamp = Timestamp(System.currentTimeMillis())
                    val text = "[Location @ $timestamp]\nLat:${location.latitude}, Lng:${location.longitude}\n"
                    textView.setText(text + textView.getText())
                }
                .addOnFailureListener { e ->
                    Log.e("TAG_SNAPSHOT", "Could not get Location: $e")
                    Toast.makeText(this@MainActivity, "Could not get Location: $e",
                            Toast.LENGTH_SHORT).show()
                }
    }

    private fun printNearbyPlaces() {
        checkFineLocationPermission(this)
        // Initialize Places.
        Places.initialize(applicationContext, getString(R.string.API_KEY))
        // Create a new Places client instance.
        val placesClient: PlacesClient = Places.createClient(this)
        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = mutableListOf(Place.Field.NAME,
                Place.Field.ADDRESS, Place.Field.TYPES, Place.Field.LAT_LNG)

        // Use the builder to create a FindCurrentPlaceRequest.
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.builder(placeFields).build()
        // Call findCurrentPlace and handle the response.
        placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
            var plText = ""
            for (placeLikelihood in response.placeLikelihoods) {
                plText += """	# ${placeLikelihood.place.name}
        likelihood: ${placeLikelihood.likelihood}
        address: ${placeLikelihood.place.address}
        placeTypes: ${placeLikelihood.place.types}
        coordinates: ${placeLikelihood.place.latLng}
    """
            }
            val timestamp = Timestamp(System.currentTimeMillis())
            val text = "\n\n[Places @ $timestamp]\n$plText"
            textView.setText(text + textView.getText())
        }.addOnFailureListener(OnFailureListener { e ->
            e.printStackTrace()
            Toast.makeText(this@MainActivity, e.localizedMessage,
                    Toast.LENGTH_SHORT).show()
        })
    }
    private inner class FenceReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action != "FENCE_RECEIVER_ACTION") {
                Log.e(
                    "TAG_FENCES", "Received an unsupported action in FenceReceiver: action=$intent.action"
                )
                return
            }
            val fenceState: FenceState = FenceState.extract(intent)
            var fenceInfo: String? = null
            when (fenceState.fenceKey) {
                "headphoneFence" -> when (fenceState.currentState) {
                    FenceState.TRUE -> fenceInfo = "TRUE | Headphones are plugged in."
                    FenceState.FALSE -> fenceInfo = "FALSE | Headphones are unplugged."
                    FenceState.UNKNOWN -> fenceInfo = "Error: unknown state."
                }
                "walkingFence" -> when (fenceState.currentState) {
                    FenceState.TRUE -> fenceInfo = "TRUE | Walking."
                    FenceState.FALSE -> fenceInfo = "FALSE | Not walking."
                    FenceState.UNKNOWN -> fenceInfo = "Error: unknown state."
                }
                "timeFence" -> when (fenceState.currentState) {
                    FenceState.TRUE -> fenceInfo = "TRUE | Within timeslot."
                    FenceState.FALSE -> fenceInfo = "FALSE | Out of timeslot."
                    FenceState.UNKNOWN -> fenceInfo = "Error: unknown state."
                }
                else -> fenceInfo = "Error: unknown fence: " + fenceState.getFenceKey()
            }
            val timestamp = Timestamp(System.currentTimeMillis())
            val text = "[Fences @ $timestamp] ${fenceState.fenceKey}: $fenceInfo".trimIndent()
            textView.text = text + "\n" + textView.text
        }
    }
}


