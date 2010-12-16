/*
 * ServeStream: A HTTP stream browser/player for Android
 * Copyright 2010 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sourceforge.servestream;

import java.util.ArrayList;

import net.sourceforge.servestream.utils.PlaylistHandler;
import net.sourceforge.servestream.utils.PreferenceConstants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class StreamMediaActivity extends Activity {
	
    private ArrayList<String> m_mediaFiles = null;
	private int m_currentMediaFileIndex = 0;
	private VideoView m_videoView = null;
    private String m_path = "";
	private int m_mediaPosition = 0;
	
	private MediaController m_mediaController = null;
	
	private SharedPreferences m_preferences = null;
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.acc_streammedia);
        
		this.setTitle(String.format("%s: %s",
				getResources().getText(R.string.app_name),
				getResources().getText(R.string.title_stream_play)));
        
        String stream = getIntent().getExtras().getString("net.sourceforge.servestream.TargetStream");
        
		m_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        if (isPlaylist(stream)) {
            PlaylistHandler playlistHandler = new PlaylistHandler(stream);
            playlistHandler.buildPlaylist();
            m_mediaFiles = playlistHandler.getPlayListFiles();
        } else {
        	m_mediaFiles = new ArrayList<String>();
        	m_mediaFiles.add(stream);
        }
        
        m_videoView = (VideoView) findViewById(R.id.surface_view);
        
        // attempt to get data from before device configuration change
        Bundle returnData = (Bundle) getLastNonConfigurationInstance();
        
        if (returnData == null) {
            m_videoView.setOnCompletionListener(m_onCompletionListener);
            m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_currentMediaFileIndex)));
            m_mediaController = new MediaController(this, true);
            
            if (m_mediaFiles.size() > 1) {
                m_mediaController.setPrevNextListeners(new nextOnClickListener(), new previousOnClickListener());
                setPlaylistButtons();
            }

            m_videoView.setOnErrorListener(m_onErrorListener);
            m_videoView.setMediaController(m_mediaController);
            Toast.makeText(StreamMediaActivity.this, "Now Playing: " + m_mediaFiles.get(m_currentMediaFileIndex), Toast.LENGTH_LONG).show();
        } else {
        	m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_currentMediaFileIndex)));
        	m_videoView.seekTo(m_mediaPosition);
        }
        
    	m_videoView.start();
        
    }
    
    private OnCompletionListener m_onCompletionListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			
			//if (m_mediaPlayerError)
			//	finish();
			
			m_currentMediaFileIndex++;
			if (m_currentMediaFileIndex == m_mediaFiles.size() - 1) {
				if (m_preferences.getString(PreferenceConstants.REPEAT, "Off").equals("All")) {
					m_currentMediaFileIndex = 0;
					setPlaylistButtons();
				    m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_currentMediaFileIndex)));
		            m_videoView.start();
					return;
				}
				finish();
			} else {
				if (m_preferences.getString(PreferenceConstants.REPEAT, "Off").equals("One")) {
					m_currentMediaFileIndex--;
				}
			    m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_currentMediaFileIndex)));
	            m_videoView.start();
	            
	            Toast.makeText(StreamMediaActivity.this, "Now Playing: " + m_mediaFiles.get(m_currentMediaFileIndex), Toast.LENGTH_LONG).show();
			}
		}
    };
	
    private OnErrorListener m_onErrorListener = new OnErrorListener() {

		@Override
		public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
			new AlertDialog.Builder(StreamMediaActivity.this)
			.setTitle(R.string.cannot_play_media_title)
			.setMessage(R.string.cannot_play_media_message)
			.setPositiveButton(R.string.cannot_play_media_pos, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
				}).create().show();
			return true;
		}
    };    
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem settings = menu.add(R.string.list_menu_settings);
		settings.setIcon(android.R.drawable.ic_menu_preferences);
		settings.setIntent(new Intent(StreamMediaActivity.this, SettingsActivity.class));

		MenuItem help = menu.add(R.string.title_help);
		help.setIcon(android.R.drawable.ic_menu_help);
		help.setIntent(new Intent(StreamMediaActivity.this, HelpActivity.class));

		return true;
	}
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        m_mediaPosition = m_videoView.getCurrentPosition();
 
        // Build bundle to save data for return
        Bundle data = new Bundle();
        data.putString("LOCATION", m_path);
        data.putInt("POSITION", m_mediaPosition);
      return data;
    }
    
    private void setPlaylistButtons() {
    	if (!(m_mediaFiles.size() == 1)) {
	 	    if (m_currentMediaFileIndex == 0) {
                m_mediaController.setPrevNextListeners(new nextOnClickListener(), null);
		    } else if (m_currentMediaFileIndex == m_mediaFiles.size() - 1) {
                m_mediaController.setPrevNextListeners(null, new previousOnClickListener());   	
		    } else {
                m_mediaController.setPrevNextListeners(new nextOnClickListener(), new previousOnClickListener());
		    }
    	}
    }
    
    private boolean isPlaylist(String streamLink) {
    	if (streamLink.length() > 4) {
    	    if (streamLink.substring(streamLink.length() - 4, streamLink.length()).equalsIgnoreCase(".m3u")) {
    	    	return true;
    	    }
    	}
    	
    	return false;
    }
    
    private class previousOnClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			m_videoView.stopPlayback();
			m_currentMediaFileIndex--;
			setPlaylistButtons();
		    m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_currentMediaFileIndex)));
		    m_videoView.start();
            Toast.makeText(StreamMediaActivity.this, "Now Playing: " + m_mediaFiles.get(m_currentMediaFileIndex), Toast.LENGTH_LONG).show();
		}
    }
    
    private class nextOnClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			m_videoView.stopPlayback();
			m_currentMediaFileIndex++;
			setPlaylistButtons();
	        m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_currentMediaFileIndex)));
	        m_videoView.start();
            Toast.makeText(StreamMediaActivity.this, "Now Playing: " + m_mediaFiles.get(m_currentMediaFileIndex), Toast.LENGTH_LONG).show();
		}
    }
}
