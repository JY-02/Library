package com.example.libraryproject;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MapFragment extends Fragment {

    private MapView mapView;
    private IMapController mapController;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        Configuration.getInstance().load(getContext(), getContext().getSharedPreferences("OSM", getContext().MODE_PRIVATE));


        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapView.setTilesScaledToDpi(true);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);

        mapController = mapView.getController();
        mapController.setZoom(15.0);
        GeoPoint currentLocation = new GeoPoint(37.566535, 126.977969);
        mapController.setCenter(currentLocation);

        ImageView iv_zoomIn = view.findViewById(R.id.zoom_in);
        ImageView iv_zoomOut = view.findViewById(R.id.zoom_out);

        iv_zoomIn.setOnClickListener(v -> mapView.getController().zoomIn());
        iv_zoomOut.setOnClickListener(v -> mapView.getController().zoomOut());

        if (Build.VERSION.SDK_INT >= 23) {
            if (getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), mapView);
                myLocationOverlay.enableMyLocation();
                mapView.getOverlays().add(myLocationOverlay);
            }
        }

        Marker marker = new Marker(mapView);
        marker.setPosition(currentLocation);

        mapView.getOverlays().add(marker);
        mapView.invalidate();

        loadLibraryLocations();

        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        mapView.setBuiltInZoomControls(true);
    }
    HashMap<Marker, String> libraryNames = new HashMap<>();
    private void loadLibraryLocations() {
        AsyncHttpClient client = new AsyncHttpClient();

        String apiUrl = "http://data4library.kr/api/libSrch?authKey=58127999a6eb1d354760ffc4d39d94af1462434e240b132374f637864593b700&pageNo=1&pageSize=99999";

        client.get(apiUrl, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                ArrayList<LibraryName> libraries = new ArrayList<>();

                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(new ByteArrayInputStream(responseBody), "UTF-8");

                    int eventType = xpp.getEventType();
                    LibraryName currentLibrary = null;
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (xpp.getName().equalsIgnoreCase("lib")) {
                                currentLibrary = new LibraryName(0, 0, "");
                            } else if (currentLibrary != null) {
                                if (xpp.getName().equalsIgnoreCase("latitude")) {
                                    currentLibrary.setLatitude(Double.parseDouble(xpp.nextText()));
                                } else if (xpp.getName().equalsIgnoreCase("longitude")) {
                                    currentLibrary.setLongitude(Double.parseDouble(xpp.nextText()));
                                } else if (xpp.getName().equalsIgnoreCase("libName")) {
                                    currentLibrary.setName(xpp.nextText());
                                }
                            }
                        } else if (eventType == XmlPullParser.END_TAG && currentLibrary != null) {
                            if (xpp.getName().equalsIgnoreCase("lib")) {
                                libraries.add(currentLibrary);
                                currentLibrary = null;
                            }
                        }
                        eventType = xpp.next();
                    }

                    for (LibraryName library : libraries) {
                        if (library.getLatitude() != 0 && library.getLongitude() != 0) {
                            GeoPoint libraryGeoPoint = new GeoPoint(library.getLatitude(), library.getLongitude());
                            Marker marker = new Marker(mapView);
                            marker.setPosition(libraryGeoPoint);
                            marker.setTitle(library.getLbName());
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setRelatedObject(library);

                            mapView.getOverlays().add(marker);
                            marker.setInfoWindow(new MyInfoWindow(R.layout.bubble_layout, mapView));
                            mapView.invalidate();

                            libraryNames.put(marker, library.getLbName());
                        }
                    }

                } catch (Exception e) {
                    Toast.makeText(getContext(), "도서관 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "도서관 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

    }
    class MyInfoWindow extends InfoWindow {

        public MyInfoWindow(int layoutResId, MapView mapView) {
            super(layoutResId, mapView);
        }

        @Override
        public void onOpen(Object item) {
            Marker marker = (Marker)item;
            LibraryName library = (LibraryName) marker.getRelatedObject();
            TextView title = (TextView) mView.findViewById(R.id.bubble_title);
            title.setText(library.getLbName());

            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getActivity(), library.getLbName() + " clicked!", Toast.LENGTH_SHORT).show();

                    LibraryDetailFragment libraryDetailFragment = new LibraryDetailFragment();
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_container, libraryDetailFragment);
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });
        }

        @Override
        public void onClose() {
            // 아무 작업도 수행하지 않습니다.
        }
    }

}