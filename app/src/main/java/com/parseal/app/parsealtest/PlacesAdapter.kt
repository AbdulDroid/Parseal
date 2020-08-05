package com.parseal.app.parsealtest


import android.content.Context
import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient


class PlacesAdapter(
    ctx: Context,
    private val bounds: LatLngBounds,
    private val client: PlacesClient
) : ArrayAdapter<AutocompletePrediction>(
    ctx,
    android.R.layout.simple_expandable_list_item_2,
    android.R.id.text1
), Filterable {


    private val STYLE_BOLD: CharacterStyle = StyleSpan(Typeface.BOLD)

    private var mResultList: List<AutocompletePrediction>? = null
    private var tempResult: List<AutocompletePrediction>? = null
    var selectedPlace: Place? = null

    override fun getCount(): Int {
        return mResultList?.size ?: 0
    }

    override fun getItem(position: Int): AutocompletePrediction? {
        val mResult = mResultList?.get(position)
        client.fetchPlace(FetchPlaceRequest.newInstance(mResult?.placeId ?: "", listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ID))).addOnSuccessListener {
            selectedPlace = it.place
        }.addOnFailureListener {
            selectedPlace = null
            it.printStackTrace()
        }
        return mResult
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)

        val item = getItem(position)

        val textView1 =
            row.findViewById<View>(android.R.id.text1) as TextView
        textView1.text = item!!.getPrimaryText(STYLE_BOLD)
        return row
    }




    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults? {
                val results = FilterResults()
                // Skip the autocomplete query if no constraints are given.
                // Skip the autocomplete query if no constraints are given.
                if (constraint != null) {
                    // Query the autocomplete API for the (constraint) search string.
                    mResultList = getAutoComplete(constraint)
                    if (mResultList != null) {
                        // The API successfully returned results.
                        results.values = mResultList
                        results.count = mResultList!!.size
                    }
                }
                return results
            }

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence? {
                // Override this method to display a readable result in the AutocompleteTextView
                // when clicked.
                // Override this method to display a readable result in the AutocompleteTextView
                // when clicked.
                return if (resultValue is AutocompletePrediction) {
                    resultValue.getFullText(null)
                } else {
                    super.convertResultToString(resultValue)
                }
            }
        }
    }

    private fun getAutoComplete(constraint: CharSequence): List<AutocompletePrediction>? {
        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        val token = AutocompleteSessionToken.newInstance()
        // Create a RectangularBounds object.

        // Use the builder to create a FindAutocompletePredictionsRequest.
        val request =
            FindAutocompletePredictionsRequest.builder()
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(constraint.toString())
                .build()
        client.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                for (prediction in response.autocompletePredictions) {
                    Log.i("PlacesAdapter", prediction.getPrimaryText(null).toString())
                }
                tempResult = response.autocompletePredictions
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    Log.e("PlacesAdapter", "Place not found: " + exception.statusCode)
                }
            }
        return tempResult
    }

}