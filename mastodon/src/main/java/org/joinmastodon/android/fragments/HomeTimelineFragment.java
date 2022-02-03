package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toolbar;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class HomeTimelineFragment extends StatusListFragment{
	private ImageButton fab;

	public HomeTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_with_fab);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetHomeTimeline(offset>0 ? getMaxID() : null, null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		fab=view.findViewById(R.id.fab);
		fab.setOnClickListener(this::onFabClick);
		updateToolbarLogo();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.home, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
//		Bundle args=new Bundle();
//		args.putString("account", accountID);
//		int id=item.getItemId();
//		if(id==R.id.new_toot){
//			Nav.go(getActivity(), ComposeFragment.class, args);
//		}else if(id==R.id.notifications){
//			Nav.go(getActivity(), NotificationsFragment.class, args);
//		}else if(id==R.id.my_profile){
//			args.putParcelable("profileAccount", Parcels.wrap(AccountSessionManager.getInstance().getAccount(accountID).self));
//			Nav.go(getActivity(), ProfileFragment.class, args);
//		}
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbarLogo();
	}

	@Subscribe
	public void onStatusCreated(StatusCreatedEvent ev){
		prependItems(Collections.singletonList(ev.status));
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	private void updateToolbarLogo(){
		ImageView logo=new ImageView(getActivity());
		logo.setScaleType(ImageView.ScaleType.CENTER);
		logo.setImageResource(R.drawable.logo);
		Toolbar toolbar=getToolbar();
		toolbar.addView(logo, new Toolbar.LayoutParams(Gravity.CENTER));
	}
}
