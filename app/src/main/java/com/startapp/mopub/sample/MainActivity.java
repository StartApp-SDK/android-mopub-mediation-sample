/**
 * Copyright 2020 StartApp Inc
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

package com.startapp.mopub.sample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideos;
import com.mopub.mobileads.MoPubView;
import com.mopub.mobileads.StartappConfig;
import com.mopub.mobileads.StartappExtras;
import com.mopub.nativeads.AdapterHelper;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.ViewBinder;
import com.startapp.mopub.sample.databinding.ActivityMainBinding;
import com.startapp.sdk.adsbase.StartAppSDK;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // any valid ad unit ID from your app
        final SdkConfiguration configuration = new SdkConfiguration.Builder(getString(R.string.interstitialId))
                .withLogLevel(MoPubLog.LogLevel.DEBUG)
                // You can initialize the startapp sdk here, but prefer to do that from the mopub network custom event interface
                //.withAdditionalNetwork(StartappConfig.class.getName()) // needs for MoPub-unsupported networks like StartApp
                //.withMediatedNetworkConfiguration(StartappConfig.class.getName(), Collections.singletonMap("startappAppId", "205738045"))
                .build();

        MoPub.initializeSdk(this, configuration, initSdkListener());

        // DON'T ADD THIS LINE TO YOUR REAL PROJECT, IT ENABLES TEST ADS WHICH GIVE NO REVENUE
        StartAppSDK.setTestAdsEnabled(true);
        // -----------------------------------------------------------------------------------
    }

    private SdkInitializationListener initSdkListener() {
        return new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
                initBannerAndMrec();
                initInterstitial();
                initRewarded();
                initNative();
            }
        };
    }

    @Override
    protected void onDestroy() {
        if (interstitial != null) {
            interstitial.destroy();
        }

        if (bannerView != null) {
            bannerView.destroy();
        }

        super.onDestroy();
    }

    //region Banner & Mrec common
    @Nullable
    private MoPubView bannerView;

    private void initBannerAndMrec() {
        bannerView = new MoPubView(this);
        bannerView.setId(ViewCompat.generateViewId());

        final ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        viewBinding.layout.addView(bannerView, params);

        final ConstraintSet constraints = new ConstraintSet();
        constraints.clone(viewBinding.layout);
        constraints.connect(bannerView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        constraints.centerHorizontally(bannerView.getId(), ConstraintSet.PARENT_ID);
        constraints.applyTo(viewBinding.layout);

        bannerView.setBannerAdListener(new MoPubView.BannerAdListener() {
            @Override
            public void onBannerLoaded(@NonNull MoPubView banner) {
                Toast.makeText(MainActivity.this, "onBannerLoaded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBannerFailed(@NonNull MoPubView banner, @NonNull MoPubErrorCode errorCode) {
                Toast.makeText(MainActivity.this, "onBannerFailed, errorCode=" + errorCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBannerClicked(@NonNull MoPubView banner) {
                Toast.makeText(MainActivity.this, "onBannerClicked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBannerExpanded(@NonNull MoPubView banner) {
                Toast.makeText(MainActivity.this, "onBannerExpanded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBannerCollapsed(@NonNull MoPubView banner) {
                Toast.makeText(MainActivity.this, "onBannerCollapsed", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //endregion

    //region Banner
    /**
     * you can as well write to mopub network custom event interface optional parameter
     * which must be in json format, unused fields can be omitted:
     * {startappAppId:'205738045', adTag:'bannerTagFromServer', minCPM:0.02, is3DBanner:false}
     * each value from the mopub interface overrides corresponding value from the extras map
     */
    public void onClickLoadBanner(@NonNull View view) {
        if (bannerView == null) {
            return;
        }

        bannerView.setAdUnitId(getResources().getString(R.string.bannerId));
        bannerView.setAdSize(MoPubView.MoPubAdSize.HEIGHT_50);

        // optionally you can set additional parameters for Startapp banner
        final Map<String, Object> extras = new StartappExtras.Builder()
                .setAdTag("bannerTagFromAdRequest")
                .enable3DBanner()
                .setMinCPM(0.01)
                .toMap();

        bannerView.setLocalExtras(extras);

        // optionally adding location & keywords
        final Location location = new Location("sample");
        location.setLatitude(40.7);
        location.setLongitude(-73.9);
        location.setAccuracy(100);

        bannerView.setLocation(location);
        bannerView.setKeywords("gender:m,age:32");

        bannerView.loadAd();
    }
    //endregion

    //region Medium Rectangle
    /**
     * you can as well write to mopub network custom event interface optional parameter
     * which must be in json format, unused fields can be omitted:
     * {startappAppId:'205738045', adTag:'mrecTagFromServer', minCPM:0.02, is3DBanner:false}
     * each value from the mopub interface overrides corresponding value from the extras map
     */
    public void onClickLoadMrec(@NonNull View view) {
        if (bannerView == null) {
            return;
        }

        bannerView.setAdUnitId(getResources().getString(R.string.mrecId));
        bannerView.setAdSize(MoPubView.MoPubAdSize.HEIGHT_250);

        // optionally you can set additional parameters for Startapp mrec
        final Map<String, Object> extras = new StartappExtras.Builder()
                .setAdTag("mrecTagFromAdRequest")
                .enable3DBanner()
                .setMinCPM(0.01)
                .toMap();

        bannerView.setLocalExtras(extras);

        // optionally adding location & keywords
        final Location location = new Location("sample");
        location.setLatitude(40.7);
        location.setLongitude(-73.9);
        location.setAccuracy(100);

        bannerView.setLocation(location);
        bannerView.setKeywords("gender:m,age:32");

        bannerView.loadAd();
    }
    //endregion

    //region Interstitial
    @Nullable
    private MoPubInterstitial interstitial;

    private void initInterstitial() {
        interstitial = new MoPubInterstitial(this, getString(R.string.interstitialId));
        interstitial.setInterstitialAdListener(new MoPubInterstitial.InterstitialAdListener() {
            @Override
            public void onInterstitialLoaded(MoPubInterstitial interstitial) {
                viewBinding.interstitialShowButton.setEnabled(true);
            }

            @Override
            public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
                Toast.makeText(MainActivity.this, "onInterstitialFailed, errorCode=" + errorCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInterstitialShown(MoPubInterstitial interstitial) {
                Toast.makeText(MainActivity.this, "onInterstitialShown", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInterstitialClicked(MoPubInterstitial interstitial) {
                Toast.makeText(MainActivity.this, "onInterstitialClicked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInterstitialDismissed(MoPubInterstitial interstitial) {
                viewBinding.interstitialShowButton.setEnabled(false);
                Toast.makeText(MainActivity.this, "onInterstitialDismissed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * you can as well write to mopub network custom event interface optional parameter
     * which must be in json format, unused by you fields can be omitted:
     * {startappAppId:'205738045', adTag:'interstitialTagFromServer', interstitialMode:'OVERLAY', minCPM:0.02, muteVideo:false}
     * each value from the mopub interface overrides corresponding value from the extras map
     */
    public void onClickLoadInterstitial(@NonNull View view) {
        if (interstitial == null) {
            return;
        }

        // optionally you can set additional parameters for Startapp interstitial
        final Map<String, Object> extras = new StartappExtras.Builder()
                .setAdTag("interstitialTagFromAdRequest")
                .setInterstitialMode(StartappExtras.Mode.OFFERWALL)
                .muteVideo()
                .setMinCPM(0.01)
                .toMap();

        interstitial.setLocalExtras(extras);

        // optionally adding keywords
        interstitial.setKeywords("gender:m,age:27");
        interstitial.load();
    }

    public void onClickShowInterstitial(@NonNull View view) {
        if (interstitial == null || !interstitial.isReady()) {
            return;
        }

        interstitial.show();
    }
    //endregion

    //region Rewarded Video
    private void initRewarded() {
        MoPubRewardedVideos.setRewardedVideoListener(new MoPubRewardedVideoListener() {
            @Override
            public void onRewardedVideoLoadSuccess(@NonNull String adUnitId) {
                viewBinding.showRewardedButton.setEnabled(true);

                Toast.makeText(MainActivity.this, "onRewardedVideoLoadSuccess", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRewardedVideoLoadFailure(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
                viewBinding.showRewardedButton.setEnabled(false);

                Toast.makeText(MainActivity.this,
                        "onRewardedVideoLoadFailure, errorCode=" + errorCode,
                        Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onRewardedVideoStarted(@NonNull String adUnitId) {
                Toast.makeText(MainActivity.this, "onRewardedVideoStarted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRewardedVideoPlaybackError(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
                Toast.makeText(MainActivity.this,
                        "onRewardedVideoPlaybackError, errorCode=" + errorCode,
                        Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onRewardedVideoClicked(@NonNull String adUnitId) {
                Toast.makeText(MainActivity.this, "onRewardedVideoClicked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRewardedVideoClosed(@NonNull String adUnitId) {
                Toast.makeText(MainActivity.this, "onRewardedVideoClosed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds, @NonNull MoPubReward reward) {
                Toast.makeText(MainActivity.this,
                        "onRewardedVideoCompleted, " + reward.getLabel() + ", " + reward.getAmount(),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    /**
     * you can as well write to mopub network custom event interface optional parameter
     * which must be in json format, unused by you fields can be omitted:
     * {startappAppId:'205738045', adTag:'rewardedTagFromServer', minCPM:0.02, muteVideo:false}
     * each value from the mopub interface overrides corresponding value from the extras map
     */
    public void onClickLoadRewarded(@NonNull View view) {
        // optionally you can set additional parameters for Startapp rewarded
        final StartappExtras.LocalExtras extras = new StartappExtras.Builder()
                .setAdTag("rewardedTagFromAdRequest")
                .muteVideo()
                .setMinCPM(0.01)
                .toMap();

        MoPubRewardedVideos.loadRewardedVideo(getResources().getString(R.string.rewardedId), extras);
    }

    public void onClickShowRewarded(@NonNull View view) {
        viewBinding.showRewardedButton.setEnabled(false);

        if (MoPubRewardedVideos.hasRewardedVideo(getResources().getString(R.string.rewardedId))) {
            MoPubRewardedVideos.showRewardedVideo(getResources().getString(R.string.rewardedId));
        } else {
            Toast.makeText(MainActivity.this, "no rewarded video", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

    //region Native
    @Nullable
    private MoPubNative moPubNative;

    @Nullable
    private NativeAd loadedNativeAd;

    private void initNative() {
        moPubNative = new MoPubNative(this, getResources().getString(R.string.nativeId), new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(@NonNull NativeAd nativeAd) {
                Toast.makeText(MainActivity.this, "onNativeLoad", Toast.LENGTH_SHORT).show();

                loadedNativeAd = nativeAd;
                loadedNativeAd.setMoPubNativeEventListener(new NativeAd.MoPubNativeEventListener() {
                    @Override
                    public void onImpression(View view) {
                        Toast.makeText(MainActivity.this, "native:onImpression", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onClick(View view) {
                        Toast.makeText(MainActivity.this, "native:onClick", Toast.LENGTH_SHORT).show();
                    }
                });

                viewBinding.showNativeButton.setEnabled(true);
            }

            @Override
            public void onNativeFail(@NonNull NativeErrorCode errorCode) {
                Toast.makeText(MainActivity.this,
                        "onNativeFail, errorCode=" + errorCode,
                        Toast.LENGTH_SHORT)
                        .show();

                loadedNativeAd = null;
                viewBinding.showNativeButton.setEnabled(false);
            }
        });

        final ViewBinder viewBinder = new ViewBinder.Builder(R.layout.native_ad_list_item)
                .mainImageId(R.id.native_main_image)
                .iconImageId(R.id.native_icon_image)
                .titleId(R.id.native_title)
                .textId(R.id.native_text)
                .callToActionId(R.id.native_cta)
                .sponsoredTextId(R.id.native_sponsored_text_view)
                .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                .build();

        final MoPubStaticNativeAdRenderer renderer = new MoPubStaticNativeAdRenderer(viewBinder);
        moPubNative.registerAdRenderer(renderer);
    }

    /**
     * you can as well write to mopub network custom event interface optional parameter
     * which must be in json format, unused fields can be omitted:
     * {startappAppId:'205738045', adTag:'nativeTagFromServer', minCPM:0.02, nativeImageSize:'SIZE150X150', nativeSecondaryImageSize:'SIZE340X340'}
     * each value from the mopub interface overrides corresponding value from the extras map
     */
    public void onClickLoadNative(@NonNull View view) {
        if (moPubNative == null) {
            return;
        }

        // clear previous ad
        viewBinding.nativeAdPlaceholder.removeAllViews();

        // optionally you can set additional parameters for Startapp native
        final Map<String, Object> extras = new StartappExtras.Builder()
                .setAdTag("nativeTagFromAdRequest")
                .setMinCPM(0.01)
                .setNativeImageSize(StartappExtras.Size.SIZE72X72)
                .setNativeSecondaryImageSize(StartappExtras.Size.SIZE150X150)
                .toMap();

        moPubNative.setLocalExtras(extras);

        final EnumSet<RequestParameters.NativeAdAsset> desiredAssets = EnumSet.of(
                RequestParameters.NativeAdAsset.TITLE,
                RequestParameters.NativeAdAsset.TEXT,
                RequestParameters.NativeAdAsset.CALL_TO_ACTION_TEXT,
                RequestParameters.NativeAdAsset.MAIN_IMAGE,
                RequestParameters.NativeAdAsset.ICON_IMAGE,
                RequestParameters.NativeAdAsset.SPONSORED,
                RequestParameters.NativeAdAsset.STAR_RATING);

        // optionally adding location & keywords
        final Location location = new Location("sample");
        location.setLatitude(44.4);
        location.setLongitude(77.7);
        location.setAccuracy(100);

        final RequestParameters requestParameters = new RequestParameters.Builder()
                .keywords("gender:f,age:18")
                .location(location)
                .desiredAssets(desiredAssets)
                .build();

        moPubNative.makeRequest(requestParameters);
    }

    public void onClickShowNative(@NonNull View view) {
        if (loadedNativeAd == null) {
            return;
        }

        final AdapterHelper adapterHelper = new AdapterHelper(MainActivity.this, 0, 2);
        final View adView = adapterHelper.getAdView(null, null, loadedNativeAd,
                new ViewBinder.Builder(0)
                        .build());

        viewBinding.nativeAdPlaceholder.removeAllViews();
        viewBinding.nativeAdPlaceholder.addView(adView);

        view.setEnabled(false);
    }
    //endregion
}
