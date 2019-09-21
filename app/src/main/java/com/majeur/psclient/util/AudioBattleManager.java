package com.majeur.psclient.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

import com.majeur.psclient.R;
import com.majeur.psclient.model.BasePokemon;

import java.io.IOException;

import static com.majeur.psclient.model.Id.toId;

public class AudioBattleManager implements AudioManager.OnAudioFocusChangeListener {

    private final boolean mCompatAudio = Build.VERSION.SDK_INT < Build.VERSION_CODES.O;

    private Context mContext;
    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private AudioFocusRequest mAudioFocusRequest;
    private AudioAttributes mMusicAudioAttrs;

    private boolean mPlaybackDelayed;
    private boolean mPlaybackNowAuthorized;
    private boolean mResumeOnFocusGain;
    private boolean mUserHasPaused;

    public AudioBattleManager(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMusicAudioAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mPlaybackDelayed) {
                    mPlaybackDelayed = false;
                    mPlaybackNowAuthorized = true;
                    if (mUserHasPaused) {
                        mUserHasPaused = false;
                        resumePlayback();
                    } else {
                        startPlayback();
                    }
                } else if (mResumeOnFocusGain) {
                    mResumeOnFocusGain = false;
                     resumePlayback();
                }

                if (mCompatAudio)
                    mMediaPlayer.setVolume(1f, 1f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Special behavior here. As long as we are not a media app, there is no way for the user to
                // resume the playback when focus is lost. So we do not stop our playback when focus is lost.
                // But we still manually abandon focus to make sure we will make a new request when starting a
                // new playback.
                mResumeOnFocusGain = false;
                mPlaybackDelayed = false;

                int result = abandonAudioFocus();
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    mPlaybackNowAuthorized = false;

                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mResumeOnFocusGain = true;
                mPlaybackDelayed = false;

                pausePlayback(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mCompatAudio)
                    mMediaPlayer.setVolume(0.15f, 0.15f);
                break;

        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private int requestAudioFocus() {
        if (mCompatAudio) {
            return mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        } else {
            mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mMusicAudioAttrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            return mAudioManager.requestAudioFocus(mAudioFocusRequest);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private int abandonAudioFocus() {
        if (mCompatAudio) {
            return mAudioManager.abandonAudioFocus(this);
        } else {
            return mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
        }
    }

    public void playPokemonCry(BasePokemon pokemon) {
        // We do not ask for audio focus here cause it should have been requested by the music part
        // TODO Check for delayed focus
        // Also as long as cries are really short, we do not take care of pausing it if user leaves
        String species = toId(pokemon.baseSpecies) + ("mega".equals(pokemon.forme) ? "-mega" : "");
        String url = cryUrl(species);
        MediaPlayer mediaPlayer = newMediaPlayer(url);
        if (mediaPlayer == null) return;
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });
    }

    private String cryUrl(String species) {
        return "https://play.pokemonshowdown.com/audio/cries/" + species + ".mp3";
    }

    public void playBattleMusic() {
        if (mPlaybackDelayed)
            return;

        if (isPlayingBattleMusic())
            return;

        int result = requestAudioFocus();
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            mPlaybackNowAuthorized = false;
        } else if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mPlaybackNowAuthorized = true;
            if (mUserHasPaused) {
                mUserHasPaused = false;
                resumePlayback();
            } else {
                startPlayback();
            }
        } else if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
            mPlaybackDelayed = true;
            mPlaybackNowAuthorized = false;
        }
    }

    public void pauseBattleMusic() {
        if (!isPlayingBattleMusic())
            return;

        pausePlayback(true);
        mUserHasPaused = true;
    }

    public void stopBattleMusic() {
        if (!isPlayingBattleMusic())
            return;

        stopPlayback();
    }

    public boolean isPlayingBattleMusic() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    private boolean mFirstPlayerHasComplete;
    private void startPlayback() {
        final MediaPlayer mediaPlayer1 = newMediaPlayer(R.raw.battle_sm_intro);
        final MediaPlayer mediaPlayer2 = newMediaPlayer(R.raw.battle_sm_loop);

        mFirstPlayerHasComplete = false;
        mediaPlayer1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
        mediaPlayer1.setAudioAttributes(mMusicAudioAttrs);
        mediaPlayer1.prepareAsync();
        mediaPlayer1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mFirstPlayerHasComplete = true;
                mMediaPlayer = mediaPlayer2;
                mediaPlayer.release();
            }
        });
        mMediaPlayer = mediaPlayer1;

        mediaPlayer2.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(true);
                if (mFirstPlayerHasComplete)
                    mediaPlayer.start();
                else
                    mediaPlayer1.setNextMediaPlayer(mediaPlayer);
            }
        });
        mediaPlayer2.setAudioAttributes(mMusicAudioAttrs);
        mediaPlayer2.prepareAsync();
    }

    private void pausePlayback(boolean abandonFocus) {
        if (mPlaybackNowAuthorized) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying())
                mMediaPlayer.pause();

            if (abandonFocus) {
                int result = abandonAudioFocus();
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    mPlaybackNowAuthorized = false;
            }
        }
    }

    private void resumePlayback() {
        if (mPlaybackNowAuthorized) {
            if (mMediaPlayer != null)
                mMediaPlayer.start();
        }
    }

    private void stopPlayback() {
        if (mPlaybackNowAuthorized) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            int result = abandonAudioFocus();
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                mPlaybackNowAuthorized = false;
        }
    }

    private MediaPlayer newMediaPlayer(int resId) {
        try {
            AssetFileDescriptor assetFileDescriptor = mContext.getResources().openRawResourceFd(resId);
            if (assetFileDescriptor == null) return null;

            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            assetFileDescriptor.close();
            return mediaPlayer;
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    private MediaPlayer newMediaPlayer(String path) {
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            return mediaPlayer;
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }
}
