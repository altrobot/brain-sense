package net.wehebs.brainsense;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.example.games.basegameutils.BaseGameActivity;

//public class MainActivity extends Activity
//public class MainActivity extends BaseGameActivity implements View.OnClickListener
public class MainActivity extends BaseGameActivity 
{
	public enum STATE
	{
		INITIAL, RUNNING, STOPPED
	};

	// // member variables
	// finalized variables
	final int MIN_TARGET = 5;
	final int MAX_TARGET = 12;
	final float MIN_AGE = 0.f;
	final float MAX_AGE = 99.9f;
	final float DIFF_WEIGHT = 20.f;
	final float EQ_Y_SECT = 7.67f;
	final float EQ_SLOPE = 0.03f;
	final float MONTH2YEAR = 1.f / 12.f;
	final int BOARD_REQUEST_CODE= 1;
	final int ACHIEVEMENT_REQUEST_CODE= 2;
	
	final long[] ACCURACY_THRESH= { 1, 10, 1000, 2000 };  
	
	// variables
	STATE state_ = STATE.INITIAL;
	int target_time_ = 0;
	long start_time_ = 0L;
	float age_ = 0.f;
	long accuracy_= Long.MAX_VALUE; 
	long user_best_= accuracy_;

	// UI instances
	TextView scene1_target_msg_;
	TextView scene1_target_min_;
	TextView scene1_target_sec_;
	TextView scene1_target_millisec_;
	TextView scene1_start_;
	ImageView scene1_circie_;
	TextView scene2_stop_;
	ImageButton scene2_lap_;
	TextView scene3_target_msg_;
	TextView scene3_target_sec_;
	TextView scene3_guess_msg_;
	TextView scene3_age_msg_;
	TextView scene3_age_;
	ImageButton scene3_reset_;
	ImageButton scene3_share_;
	ImageView scene3_brain_;
	SignInButton scene3_signin_;
//	Button scene3_signout_;
	ImageButton scene3_leader_;
	
	// sound
	boolean sound_on_= true;
	MediaPlayer mplayer_start_;
	MediaPlayer mplayer_stop_;
	MediaPlayer mplayer_reset_;
	MediaPlayer mplayer_share_;
	MediaPlayer mplayer_board_;
	
	Context context_ = null;

	// ad
	AdView ad_view_;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	    
		// reset the auto sign-in
		getGameHelper().setMaxAutoSignInAttempts(0);
		
		// Look up the AdView as a resource and load a request.
		ad_view_= (AdView)this.findViewById(R.id.adView);
	    AdRequest adRequest= new AdRequest.Builder()
//	            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
//	            .addTestDevice("7C5312BDE3036F3B6D664E1D85B62F19")
//	    		.addTestDevice("B435E44ECDC06C718CC9A21F786C871C")
	    		.build();
	    ad_view_.loadAd(adRequest);

		
		// this causes an exception 
//		context_ = getApplicationContext();
		context_= MainActivity.this;
		
		// // get UI instances
		// get instances of scene1
		scene1_target_msg_ = (TextView) findViewById(R.id.scene1_target_msg);
		scene1_target_min_ = (TextView) findViewById(R.id.scene1_target_min);
		scene1_target_sec_ = (TextView) findViewById(R.id.scene1_target_sec);
		scene1_target_millisec_ = (TextView) findViewById(R.id.scene1_target_millisec);
		scene1_start_ = (TextView) findViewById(R.id.scene1_start);
		scene1_circie_ = (ImageView) findViewById(R.id.scene1_circle);
		// get instances of scene2
		scene2_stop_ = (TextView) findViewById(R.id.scene2_stop);
		scene2_lap_ = (ImageButton) findViewById(R.id.scene2_lap);
		// get instances of scene3
		scene3_target_msg_ = (TextView) findViewById(R.id.scene3_target_msg);
		scene3_target_sec_ = (TextView) findViewById(R.id.scene3_target_sec);
		scene3_guess_msg_ = (TextView) findViewById(R.id.scene3_guess_msg);
		scene3_age_msg_ = (TextView) findViewById(R.id.scene3_age_msg);
		scene3_age_ = (TextView) findViewById(R.id.scene3_age);
		scene3_reset_ = (ImageButton) findViewById(R.id.scene3_reset);
		scene3_share_ = (ImageButton) findViewById(R.id.scene3_share);
		scene3_brain_ = (ImageView) findViewById(R.id.scene3_brain);
		scene3_signin_= (SignInButton)findViewById(R.id.sign_in_button);
//		scene3_signout_= (Button)findViewById(R.id.sign_out_button);
		scene3_leader_= (ImageButton)findViewById(R.id.leaderboard);
		//
		// // // make the UIs in scenes other than scene1
		showUIs(1, View.INVISIBLE);
		showUIs(2, View.INVISIBLE);

		String str_min = String.format("%01d", 0);
		String str_millisec = String.format("%03d", 0);
		scene1_target_min_.setText(str_min);
		scene1_target_millisec_.setText(str_millisec);
		// // set a target time with random number generation
		// get an uniform random number
		target_time_ = getRandomNumber(MIN_TARGET, MAX_TARGET);
		String target_str = String.format("%02d", target_time_);
		scene1_target_sec_.setText(target_str);
		// show UIs of scene1
		showUIs(0, View.VISIBLE);

		// handle start/stop button event
		scene1_circie_.setOnClickListener(listener_image_);

		// handle reset button event
		scene3_reset_.setOnClickListener(listener_reset_);

		// handle share button event
		scene3_share_.setOnClickListener(listener_share_);

		// handle brain button event
		scene3_brain_.setOnClickListener(listener_brain_);
		
		// create media player
		mplayer_start_= MediaPlayer.create( getApplicationContext(), R.raw.start_sound );
		mplayer_stop_= MediaPlayer.create( getApplicationContext(), R.raw.stop_sound );
		mplayer_reset_= MediaPlayer.create( getApplicationContext(), R.raw.reset_sound );
		mplayer_share_= MediaPlayer.create( getApplicationContext(), R.raw.share_sound );
		mplayer_board_= MediaPlayer.create( getApplicationContext(), R.raw.board_sound );
		// google log in/out
		scene3_signin_.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick( View _view )
			{
				// google account sign in
				beginUserInitiatedSignIn();
			}
		} );
		
		// leaderboard button
		scene3_leader_.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick( View _view )
			{
				if( sound_on_ )
					mplayer_board_.start();
				
				// login check
				// if not logged in, log in first
				if( !getApiClient().isConnected() )
				{
					Toast.makeText( context_, R.string.no_signin_msg, Toast.LENGTH_SHORT ).show();
					beginUserInitiatedSignIn();
					return;							
				}

				//// popup for asking to submit the result
				// create a dialog 
				AlertDialog.Builder dialog = new AlertDialog.Builder( context_ );
				dialog.setIcon(R.drawable.crown);
				dialog.setTitle(getResources().getString(R.string.submit_title));
				dialog.setMessage(getResources().getString(R.string.submit_msg));
				dialog.setPositiveButton( "Submit", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						// read the user's best result
						loadScoreOfLeaderBoard();
						// open dialog if the new result is smaller than the record
						if( accuracy_ < user_best_ )
						{
							// submit the result
							Games.Leaderboards.submitScore( getApiClient(), getString(R.string.leaderboard), accuracy_ );
						}
						else
						{
							dialog.cancel();
						}
		        	    startActivityForResult(Games.Leaderboards.getLeaderboardIntent(
		        		        getApiClient(), getString(R.string.leaderboard)), BOARD_REQUEST_CODE );

					}
				} );
				dialog.setNeutralButton("Achievements", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						unlockAchievement();
						startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), ACHIEVEMENT_REQUEST_CODE );
					}
				} );
				dialog.setNegativeButton( "Rate", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick( DialogInterface dialog, int which )
					{
						Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse(getString(R.string.rate_url)) );
						startActivity(intent);
					}
				} );
				
				dialog.show();
				
			}
		} );
		
	}

	private void loadScoreOfLeaderBoard()
	{
	    Games.Leaderboards.loadCurrentPlayerLeaderboardScore( getApiClient(), getString(R.string.leaderboard), 
	    		LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC)
	    		.setResultCallback( new ResultCallback<Leaderboards.LoadPlayerScoreResult>()
	    {
	        @Override
	        public void onResult(final Leaderboards.LoadPlayerScoreResult scoreResult)
	        {
	            if( isScoreResultValid( scoreResult ) )
	            {
	                user_best_= scoreResult.getScore().getRawScore();
	            }
	            else
	            {
	            	user_best_= Long.MAX_VALUE;
	            }
	        }
	    });
	}

	private boolean isScoreResultValid(final Leaderboards.LoadPlayerScoreResult scoreResult)
	{
	    return scoreResult != null && GamesStatusCodes.STATUS_OK == scoreResult.getStatus().getStatusCode() && scoreResult.getScore() != null;
	}

	@Override
	public void onSignInSucceeded()
	{
	    findViewById(R.id.sign_in_button).setVisibility(View.GONE);
//	    findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
	}
	 
	@Override
	public void onSignInFailed()
	{
	    findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
//	    findViewById(R.id.sign_out_button).setVisibility(View.GONE);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		// set the sound on for default
		sound_on_= true;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id= item.getItemId();
		if( id == R.id.toggle_sound )
		{
			sound_on_= !sound_on_;
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
//		menu.findItem(R.id.radio1).setChecked(true);
		
		return true;
	}

	OnClickListener listener_image_ = new OnClickListener()
	{
		@Override
		public void onClick(View _view)
		{
			// if the button state is INITIAL
			if (state_ == STATE.INITIAL)
			{
				// play start sound
				if( sound_on_ )
					mplayer_start_.start();
				
				// transit the state
				state_ = STATE.RUNNING;
				// start measuring time
				start_time_ = SystemClock.uptimeMillis();
				// hide UIs
				showUIs(0, View.INVISIBLE);
				showUIs(2, View.INVISIBLE);
				// show UIs of scene1
				showUIs(1, View.VISIBLE);

				// Toast test
				// Toast.makeText( getActivity().getApplicationContext(),
				// "start clicked", Toast.LENGTH_SHORT).show();
			}
			// if the button state is running
			else if (state_ == STATE.RUNNING)
			{
				// play stop sound
				if( sound_on_ )
					mplayer_stop_.start();

				// transit the state
				state_ = STATE.STOPPED;
				// get the elapsed time in milliseconds
				long elapsed_time = SystemClock.uptimeMillis() - start_time_;
				// compute the accuracy
				accuracy_= Math.abs( elapsed_time - target_time_*1000 ); 
				// // compute the brain age
				// compute the difference ratio after converting the
				// target_time_ in milliseconds
				float diff_ratio = (target_time_ * 1000.f - elapsed_time)
						/ (target_time_ * 1000.f);
				// since the diff_ratio is too small in measuring a very short
				// time, need to weight it
				diff_ratio *= DIFF_WEIGHT;
				// compute the age
				// float age= (EQ_Y_SECT - diff_ratio)/EQ_SLOPE*MONTH2YEAR;
				age_ = (EQ_Y_SECT - diff_ratio) / EQ_SLOPE * MONTH2YEAR;
				// set limits
				if (age_ < 0.f)
					age_ = MIN_AGE;
				if (age_ > 100.f)
					age_ = MAX_AGE;
				String str_age = String.format("%02.1f", age_);
				// set the age on the screen
				scene3_age_.setText(str_age);
				// parse the time in min, sec, millisec
				int seconds = (int) (elapsed_time / 1000);
				int minutes = seconds / 60;
				seconds = seconds % 60;
				int milliseconds = (int) (elapsed_time % 1000);
				String str_min = String.format("%01d", minutes);
				String str_sec = String.format("%02d", seconds);
				String str_millisec = String.format("%03d", milliseconds);
				// set the time
				scene1_target_min_.setText(str_min);
				scene1_target_sec_.setText(str_sec);
				scene1_target_millisec_.setText(str_millisec);
				String str_target = String.format("%02d", target_time_);
				scene3_target_sec_.setText(str_target);
				// // show and hide UIs
				// hide UIs other than ones of scene3
				showUIs(0, View.INVISIBLE);
				showUIs(1, View.INVISIBLE);
				// show UIs of the scene3
				showUIs(2, View.VISIBLE);
				// make the image not clickable
				scene1_circie_.setClickable(false);			
				// display the accuracy
				String str_accuracy= String.format("Your response ability: %d ms. Click the Crown Image to submit.", accuracy_ );
				Toast.makeText( context_, str_accuracy, Toast.LENGTH_SHORT).show();
				// check if the user is signed in
				// if not, ask to sign in
				if( !getApiClient().isConnected() )
				{
					Toast.makeText( context_, R.string.no_signin_msg_achievement, Toast.LENGTH_LONG ).show();
					return;							
				}
				// unlock an achievement
				unlockAchievement();
			}
		}
	};
	
	// unlock achievement
	private void unlockAchievement()
	{
		// check if the user is signed in
		// if not, ask to sign in
		if( !getApiClient().isConnected() )
		{
			return;							
		}
		// compute the level and unlock the related achievement
		if( accuracy_ < ACCURACY_THRESH[0] )
		{
			Games.Achievements.unlock( getApiClient(), getString(R.string.achievement_lv5) );
		}
		else if( accuracy_ < ACCURACY_THRESH[1] )
		{
			Games.Achievements.unlock( getApiClient(), getString(R.string.achievement_lv4) );
		}
		else if( accuracy_ < ACCURACY_THRESH[2] )
		{
			Games.Achievements.unlock( getApiClient(), getString(R.string.achievement_lv3) );
		}
		else if( accuracy_ < ACCURACY_THRESH[3] )
		{
			Games.Achievements.unlock( getApiClient(), getString(R.string.achievement_lv2) );
		}
		else
		{
			Games.Achievements.unlock( getApiClient(), getString(R.string.achievement_lv1) );
		}
	}
	
	// callback for reset button
	OnClickListener listener_reset_ = new OnClickListener()
	{
		@Override
		public void onClick(View _view)
		{			
			//// initialize the clock
			// play reset sound
			if( sound_on_ )
				mplayer_reset_.start();
			
			// toggle the button status
			state_ = STATE.INITIAL;
			// set another random number
			target_time_ = getRandomNumber(MIN_TARGET, MAX_TARGET);
			String str_sec = String.format("%02d", target_time_);
			
			// reset accuracy
			accuracy_= Long.MAX_VALUE;
					
			// reset the time with the new random target
			String str_min = String.format("%01d", 0);
			String str_millisec = String.format("%03d", 0);
			scene1_target_min_.setText(str_min);
			scene1_target_sec_.setText(str_sec);
			scene1_target_millisec_.setText(str_millisec);
			// hide UIs other than ones in scene1
			showUIs(1, View.INVISIBLE);
			showUIs(2, View.INVISIBLE);
			// show UIs of scene1
			showUIs(0, View.VISIBLE);
			// make the image clickable
			scene1_circie_.setClickable(true);
		}
	};

	/*
	 * Callback for brain button
	 */
	OnClickListener listener_brain_ = new OnClickListener()
	{
		@Override
		public void onClick(View _view)
		{
			// create a dialog for "about"
			AlertDialog.Builder dialog = new AlertDialog.Builder(context_);
			dialog.setIcon(R.drawable.brain_icon);
			dialog.setTitle(getResources().getString(R.string.about));
			dialog.setMessage(getResources().getString(R.string.about_msg));
			dialog.setNeutralButton(getResources().getString(R.string.visit), new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://wehebs.net"));
					startActivity(intent);
				}
			} );

			dialog.show();
		}
	};

	// callback for share button
	OnClickListener listener_share_ = new OnClickListener()
	{
		@Override
		public void onClick(View _view)
		{
			// play reset sound
			if( sound_on_ )
				mplayer_share_.start();
			
			shareResult();
		}
	};

	/*
	 * Share the result
	 */
	private void shareResult()
	{
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		// / intent.setType( "text/plain" );
		intent.setType("image/jpeg");
//		String contents = String.format( "Here is my brain age: %02.1f. Isn't it surprising?", age_ );

		// Bitmap saving test
		Bitmap bitmap = takeScreenshot();
		saveBitmap(bitmap);

		String filename = getResources().getString(R.string.screenshot_name);

		Uri file_uri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + File.separator + filename));
		intent.putExtra(Intent.EXTRA_STREAM, file_uri);

//		intent.putExtra(android.content.Intent.EXTRA_TEXT, contents);
		startActivity(Intent.createChooser(intent, "Share via"));
	}

	/*
	 * Take a screenshot.
	 * 
	 * @note not using setDrawingCache(enable). Instead use buildDrawingCache()
	 * to take a shot only one time.
	 */
	public Bitmap takeScreenshot()
	{
		View root_view= findViewById(android.R.id.content).getRootView();
		root_view.buildDrawingCache();
		return root_view.getDrawingCache();
	}

	/*
	 * Save the screenshot.
	 * 
	 * @param _bitmap bitmap image
	 */
	public void saveBitmap(Bitmap _bitmap)
	{
		String filename = getResources().getString(R.string.screenshot_name);
		File image_path = new File(Environment.getExternalStorageDirectory() + File.separator + filename);

		FileOutputStream fos;
		try
		{
			fos = new FileOutputStream(image_path);
			_bitmap.compress(CompressFormat.JPEG, 100, fos);
			fos.flush();
			fos.close();
		}
		catch (FileNotFoundException e)
		{
		} 
		catch (IOException e)
		{
		}
	}

	/*
	 * Show UIs
	 * 
	 * @param _scene scene number to show UIs
	 */
	private void showUIs(final int _scene, int _option)
	{
		switch (_scene)
		{
		case 0:
			scene1_target_msg_.setVisibility(_option);
			scene1_target_min_.setVisibility(_option);
			scene1_target_sec_.setVisibility(_option);
			scene1_target_millisec_.setVisibility(_option);
			scene1_start_.setVisibility(_option);
			scene1_circie_.setVisibility(_option);
			break;
		case 1:
			scene1_circie_.setVisibility(_option);
			scene2_stop_.setVisibility(_option);
			scene2_lap_.setVisibility(_option);
			break;
		case 2:
			// the min, sec, millisec in the middle are shared with scene3
			scene1_target_min_.setVisibility(_option);
			scene1_target_sec_.setVisibility(_option);
			scene1_target_millisec_.setVisibility(_option);
			scene1_circie_.setVisibility(_option);
			scene3_target_msg_.setVisibility(_option);
			scene3_target_sec_.setVisibility(_option);
			scene3_guess_msg_.setVisibility(_option);
			scene3_age_msg_.setVisibility(_option);
			scene3_age_.setVisibility(_option);
			scene3_reset_.setVisibility(_option);
			scene3_share_.setVisibility(_option);
			scene3_brain_.setVisibility(_option);
			if( (getApiClient().isConnected() && _option == View.INVISIBLE) ||
					(!getApiClient().isConnected() && _option == View.VISIBLE))
				scene3_signin_.setVisibility(_option);
			scene3_leader_.setVisibility(_option);
			break;
		default:
			break;
		}
	}

	@Override
	public void onResume()
	{
	    super.onResume();
	    if( ad_view_ != null )
	    {
	    	ad_view_.resume();
	    }
	}

	@Override
	public void onPause()
	{
	    if( ad_view_ != null )
	    {
	    	ad_view_.pause();
	    }
	    super.onPause();
	 }
	
	@Override
	protected void onDestroy()
	{
		// Destroy the AdView.
	    if( ad_view_ != null )
	    {
	    	ad_view_.destroy();
	    }
		mplayer_start_.release();
		mplayer_stop_.release();
		mplayer_reset_.release();
		mplayer_share_.release();	
		mplayer_board_.release();

		super.onDestroy();
	}

	/*
	 * Generate random number with a range
	 * @param _min lower limit of the range
	 * @param _max upper limit of the range
	 * @return resulting random number
	 */
	public int getRandomNumber(final int _min, final int _max)
	{
		Random random_generator = new Random();
		return random_generator.nextInt((_max - _min) + 1) + _min;
	}
}
