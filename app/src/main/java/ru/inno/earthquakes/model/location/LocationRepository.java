package ru.inno.earthquakes.model.location;

import android.content.Context;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import io.reactivex.Single;
import ru.inno.earthquakes.entities.Location;

/**
 * @author Artur Badretdinov (Gaket) 22.07.17.
 */
public class LocationRepository {

  private final FusedLocationProviderClient fusedLocationClient;
  private final GoogleApiAvailability googleApiAvailability;
  private final Context context;
  private boolean isGoogleApiAvailable = true;

  public LocationRepository(FusedLocationProviderClient fusedLocationClient,
      GoogleApiAvailability googleApiAvailability, Context context) {
    this.fusedLocationClient = fusedLocationClient;
    this.googleApiAvailability = googleApiAvailability;
    this.context = context;
  }

  /**
   * @return Current coordinates, taken from the Android system
   * @throws {@link UnknownLocationException} if the last position is unknown
   * @throws {@link SecurityException} if we try to get position without permissions
   */
  Single<Location.Coordinates> getCurrentCoordinates() {
    return getLastLocation()
        .map(location -> new Location.Coordinates(location.getLongitude(), location.getLatitude()));
  }

  /**
   * Get last known location of the user. If there are no Google services on phone, we will have an
   * error
   */
  private Single<android.location.Location> getLastLocation() {
    return Single.create(emitter -> {
      try {
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
              if (emitter.isDisposed()) {
                return;
              }

              // GPS location can be null if GPS is switched off or we don't have play services
              if (location != null) {
                emitter.onSuccess(location);
              } else {
                emitter.onError(new UnknownLocationException("Last location is unknown"));
              }
            })
            .addOnFailureListener(emitter::onError);
      } catch (SecurityException ex) {
        emitter.onError(ex);
      }
    });
  }

  /**
   * @return if location services, needed for the app are available
   */
  boolean checkPlayServicesAvailable() {
    final int status = googleApiAvailability.isGooglePlayServicesAvailable(context);

    if (status != ConnectionResult.SUCCESS) {
      isGoogleApiAvailable = false;
    }
    return isGoogleApiAvailable;
  }

  /**
   * @return status code from the location services
   */
  int getPlayServicesStatus() {
    return googleApiAvailability.isGooglePlayServicesAvailable(context);
  }
}
