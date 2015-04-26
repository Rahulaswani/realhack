package com.rahulaswani.realhack.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pkmmte.view.CircularImageView;
import com.rahulaswani.realhack.R;

import java.lang.reflect.Array;
import java.util.Locale;

/**
 * Created by rahul on 26/04/15.
 */
public class AgentProfileFragment extends Fragment {

    public AgentProfileFragment() {
        // Empty constructor required for fragment subclasses
    }

    String[] names = {"Marathahalli",
            "BTM Layout",
            "Kormangala",
            "HSR layout",
            "Hebbal",
            "Indiranagar",
            };

    /*int[] images = {R.drawable.denereis,
            R.drawable.eye_squash,
            R.drawable.father,
            R.drawable.fuck,
            R.drawable.ironborn,
            R.drawable.jamie,
            R.drawable.jhon,
            R.drawable.littleone,
            R.drawable.sercie};*/

    LatLng newLatLng;

    double[] latitude = {12.9567746,12.9135224,12.933221,12.9102585,13.0349961,12.9731051};

    double[] longitude = {77.6983846,77.61259,77.6321925,77.6456604,77.5981732,77.638255};

    Location[] targetLocation = new Location[9];//provider name is unecessary

    ListView custom_list;
    private GoogleMap myMap;
    MapFragment myMapFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_agent_profile, container, false);

        custom_list = (ListView) rootView.findViewById(R.id.list);




        FragmentManager myFragmentManager = getFragmentManager();
        myMapFragment = (MapFragment)myFragmentManager.findFragmentById(R.id.map);

        if(myMapFragment == null) {
            myMapFragment = MapFragment.newInstance();
            FragmentTransaction fragmentTransaction = myFragmentManager.beginTransaction();
            fragmentTransaction.add(android.R.id.content, myMapFragment).commit();
        }

        MapsInitializer.initialize(getActivity().getApplicationContext());


        for(int i=0; i < 6; i++) {
            try {
                targetLocation[i].setLatitude(latitude[i]);
                targetLocation[i].setLongitude(longitude[i]);
            } catch (Exception e) {
                Log.e("rahul","latitude[i] = "+latitude[i]);
                Log.e("rahul","longitude[i] = "+longitude[i]);
            }

        }

        if(null != getActivity()) {
            myAdapter myadapter = new myAdapter(getActivity(), names);
            custom_list.setAdapter(myadapter);
            custom_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Log.e("rahul","position is : "+position);

                    //LayoutInflater inflater = getActivity().getLayoutInflater();
                    //view = inflater.inflate(R.layout.custom_list_view,parent,false);
                    //((TextView) rootView.findViewById(R.id.textview_username)).setText(names[position]);

                    try {
                        newLatLng = new LatLng(targetLocation[position].getLatitude(), targetLocation[position].getLongitude());
                        Log.e("rahul","latitude["+position+"] = "+ targetLocation[position].getLatitude());
                        Log.e("rahul","longitude["+position+"] = "+targetLocation[position].getLongitude());
                    } catch (Exception e) {

                    }
                    myMap.setMyLocationEnabled(true);
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 13));
                    myMap.addMarker(new MarkerOptions().position(newLatLng));
                }
            });
        }
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public class myAdapter extends ArrayAdapter<String> {
        int[] img;
        myAdapter(Context context,String[] names){
            super(context,R.layout.custom_list_view,R.id.textview_username,names);
           // this.img = images;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.custom_list_view,parent,false);
            ((TextView) view.findViewById(R.id.textview_username)).setText(names[position]);

            CircularImageView circularImageView = (CircularImageView) view.findViewById(R.id.imageview_userpic);
            circularImageView.setBorderColor(getResources().getColor(R.color.black_overlay));
            circularImageView.setBorderWidth(5);
            circularImageView.setSelectorColor(getResources().getColor(R.color.primary_yello));
            circularImageView.setSelectorStrokeColor(getResources().getColor(R.color.primary_yellow_dark));
            circularImageView.setSelectorStrokeWidth(10);
            circularImageView.addShadow();
          //  circularImageView.setImageResource(img[position]);
            return view;//super.getView(position, convertView, parent);
        }
    }

}