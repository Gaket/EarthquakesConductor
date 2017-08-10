package ru.inno.earthquakes.presentation.alertscreen;

import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.OnClick;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.bluelinelabs.conductor.ControllerChangeHandler;
import com.bluelinelabs.conductor.ControllerChangeType;
import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler;
import com.google.android.gms.common.GoogleApiAvailability;
import java.util.Locale;
import javax.inject.Inject;
import ru.inno.earthquakes.EarthquakeApp;
import ru.inno.earthquakes.R;
import ru.inno.earthquakes.entities.EarthquakeWithDist;
import ru.inno.earthquakes.model.earthquakes.EarthquakesInteractor;
import ru.inno.earthquakes.model.location.LocationInteractor;
import ru.inno.earthquakes.model.settings.SettingsInteractor;
import ru.inno.earthquakes.presentation.common.SchedulersProvider;
import ru.inno.earthquakes.presentation.common.Utils;
import ru.inno.earthquakes.presentation.common.controller.BaseController;
import ru.inno.earthquakes.presentation.earthquakeslist.EarthquakesListController;
import ru.inno.earthquakes.presentation.settings.SettingsController;

/**
 * @author Artur Badretdinov (Gaket) 08.08.17
 */
public class AlertController extends BaseController implements AlertView {

  // Here, and in other controllers, controller works as a root for some model components
  // dependency tree. As a result, we inject them here and deeper all injections are made
  // through the constructors.
  @InjectPresenter
  AlertPresenter presenter;
  @Inject
  EarthquakesInteractor earthquakesInteractor;
  @Inject
  LocationInteractor locationInteractor;
  @Inject
  SettingsInteractor settingsInteractor;
  @Inject
  SchedulersProvider schedulersProvider;
  @Inject
  GoogleApiAvailability googleApiAvailability;

  @BindView(R.id.alert_swipe_refresh)
  SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R.id.alert_message)
  TextView messageView;
  @BindView(R.id.alert_details)
  TextView detailsView;
  @BindView(R.id.alert_magnitude)
  TextView magnitudeView;
  @BindView(R.id.alert_distance)
  TextView distanceView;
  @BindView(R.id.alert_status)
  ImageView alertImageView;
  private Snackbar snackbar;

  @ProvidePresenter
  AlertPresenter providePresenter() {
    EarthquakeApp.getComponentsManager().getEarthquakesComponent().inject(this);
    return new AlertPresenter(earthquakesInteractor, locationInteractor, settingsInteractor,
        schedulersProvider);
  }

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_main, container, false);
  }

  @NonNull
  @Override
  protected String getTitle() {
    return getResources().getString(R.string.title_alert);
  }

  @Override
  protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);
    swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefreshAction());
    setHasOptionsMenu(true);
    showActionBarBackButton(false);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_alert, menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        presenter.onOpenSettings();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @OnClick(R.id.alert_show_all)
  void onShowAllAction() {
    presenter.onShowAll();
  }

  @Override
  public void showThereAreNoAlerts() {
    messageView.setText(R.string.alert_msg_everything_is_ok);
    alertImageView.setImageResource(R.drawable.earth_normal);
    distanceView.setVisibility(View.GONE);
    magnitudeView.setVisibility(View.GONE);
    detailsView.setText(R.string.alert_details_no_earthquakes);
  }

  @Override
  public void showEarthquakeAlert(EarthquakeWithDist earthquake) {
    alertImageView.setImageResource(R.drawable.earth_alarm);
    messageView.setText(R.string.alert_msg_earthquake_nearby);
    detailsView.setText(earthquake.getEarthquake().getTitle());
    distanceView.setText(getResources().getString(R.string.alert_distance_from_place,
        Utils.formatDistanceString(earthquake.getDistance())));
    String magnitude = String
        .format(Locale.getDefault(), "%.2f", earthquake.getEarthquake().getMagnitude());
    magnitudeView.setText(magnitude);
    distanceView.setVisibility(View.VISIBLE);
    magnitudeView.setVisibility(View.VISIBLE);
  }

  @Override
  public void showNetworkError(boolean show) {
    if (show) {
      snackbar = Snackbar.make(swipeRefreshLayout, R.string.error_connection,
          BaseTransientBottomBar.LENGTH_INDEFINITE)
          .setAction(R.string.action_ok, (d) -> snackbar.dismiss());
      snackbar.show();
    } else if (snackbar != null) {
      snackbar.dismiss();
    }
  }

  // This code is needed to hide action bar item when routing animation starts
  @Override
  protected void onChangeStarted(@NonNull ControllerChangeHandler changeHandler,
      @NonNull ControllerChangeType changeType) {
    setOptionsMenuHidden(!changeType.isEnter);

    if (changeType.isEnter) {
      setTitle();
    }
  }

  @Override
  public void showLoading(boolean show) {
    swipeRefreshLayout.setRefreshing(show);
  }

  @Override
  public void navigateToEarthquakesList() {
    getRouter().pushController(RouterTransaction.with(new EarthquakesListController())
        .pushChangeHandler(new FadeChangeHandler())
        .popChangeHandler(new FadeChangeHandler()));
  }

  @Override
  public void navigateToSettings() {
    getRouter().pushController(RouterTransaction.with(new SettingsController())
        .pushChangeHandler(new FadeChangeHandler())
        .popChangeHandler(new FadeChangeHandler()));
  }

  @Override
  public void showPermissionDeniedAlert() {
    Toast.makeText(getActivity(), R.string.error_prohibited_location_access, Toast.LENGTH_LONG)
        .show();
  }

  @Override
  public void showNoDataAlert() {
    Toast.makeText(getActivity(), R.string.error_no_data, Toast.LENGTH_LONG).show();
  }

  @Override
  public void showGoogleApiMessage(int status) {
    if (googleApiAvailability.isUserResolvableError(status)) {
      googleApiAvailability.getErrorDialog(getActivity(), status, 1).show();
    } else {
      Snackbar.make(swipeRefreshLayout, R.string.error_google_api_unavailable,
          Snackbar.LENGTH_INDEFINITE).show();
    }
  }
}
