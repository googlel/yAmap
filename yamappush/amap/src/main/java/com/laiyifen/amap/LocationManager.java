package com.laiyifen.amap;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;

/**
 * The creator is Leone && E-mail: butleone@163.com
 *
 * @author Leone
 * @date 15/10/22
 * @description Edit it! Change it! Beat it! Whatever, just do it!
 */
public class LocationManager {

    private static LocationManager mManager;
    private static AMapLocation mLocation;
    private static Float mFakeLat;
    private static Float mFakeLng;

    /**
     * 定位回调
     */
    public interface LocationListener{
        /**
         * 定位位置成功
         * @param aMapLocation aMapLocation
         */
        void onFindLocation(AMapLocation aMapLocation);

        /**
         * 定位位置失败
         */
        void onFindLocationFail(int errorCode);
    }

    private AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (aMapLocation != null && aMapLocation.getAMapException().getErrorCode() == 0) {
                LocationManager.mLocation = aMapLocation;
            }
        }

        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    /**
     * LocationManager
     */
    private LocationManager() {

    }

    /**
     * 定位单例
     * @return this
     */
    public static LocationManager getInstance() {
        if (mManager == null) {
            mManager = new LocationManager();
        }
        return mManager;
    }

    /**
     * 清除历史定位数据
     */
    public void clearFakeLocation(){
        mFakeLat = null;
        mFakeLng = null;
        mLocation = null;
    }

    /**
     * 获取定位的坐标
     * @return 返回坐标
     */
    public String getLocationXY(){
        if(mFakeLat != null && mFakeLng != null) {
            return mFakeLat + "," + mFakeLng;
        }

        if(mLocation == null) {
            return null;
        }

        if(mLocation.getLatitude() == 0 && mLocation.getLongitude() == 0) {
            return null;
        }

        return mLocation.getLatitude() + "," + mLocation.getLongitude();
    }

    /**
     * 获取定位城市名称
     * @return 城市
     */
    public String getLocationCityName(){
        return mLocation != null ? mLocation.getCity() : null;
    }

    /**
     * 获取定位地址
     * @return 地址
     */
    public String getLocationAddress(){
        return mLocation != null ? mLocation.getAddress() : null;
    }

    /**
     * 获取城市ID
     * @return id
     */
    public String getLocationCityId() {
        return mLocation != null ? mLocation.getCityCode() : null;
    }

    /**
     * 请求SDK做一次定位
     * @param context context
     */
    public void requestLocationOnce(Context context) {
        LocationManagerProxy locationProxy = LocationManagerProxy.getInstance(context);
        //此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        //注意设置合适的定位时间的间隔，并且在合适时间调用removeUpdates()方法来取消定位请求
        //在定位结束后，在合适的生命周期调用destroy()方法
        //其中如果间隔时间为-1，则定位只定一次
        locationProxy.setGpsEnable(false);
        locationProxy.requestLocationData(
                LocationProviderProxy.AMapNetwork, -1, 1000, mLocationListener);
    }

    /**
     * 历史缓存有则用，没有则定位
     * @param context context
     * @param listener listener
     */
    public void requestLocationWithCache(final Context context, final LocationListener listener) {
        if(mLocation != null){
            listener.onFindLocation(mLocation);
            //更新缓存的mLocation
            requestLocationOnce(context);
            return;
        }
        requestLocation(context, listener);
    }

    /**
     * 请求定位
     * @param context context
     * @param listener listener
     */
    public void requestLocation(final Context context, final LocationListener listener) {
        final Handler timeOutHandler = new Handler(Looper.getMainLooper());
        if(mLocation != null){
            timeOutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    listener.onFindLocation(mLocation);
                    //更新缓存的mLocation
                    requestLocationOnce(context);
                }
            }, 1000);
            return;
        }
        try {
            LocationManagerProxy locationProxy = LocationManagerProxy.getInstance(context);
            locationProxy.setGpsEnable(false);
            locationProxy.requestLocationData(LocationProviderProxy.AMapNetwork, -1, 1000, new AMapLocationListener() {
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    if (mLocationListener != null) {
                        mLocationListener.onStatusChanged(provider, status, extras);
                    }
                }

                @Override
                public void onProviderEnabled(String provider) {
                    if (mLocationListener != null) {
                        mLocationListener.onProviderEnabled(provider);
                    }
                }

                @Override
                public void onProviderDisabled(String provider) {
                    if (mLocationListener != null) {
                        mLocationListener.onProviderDisabled(provider);
                    }
                }

                @Override
                public void onLocationChanged(Location location) {
                    if (mLocationListener != null) {
                        mLocationListener.onLocationChanged(location);
                    }
                }

                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    timeOutHandler.removeCallbacksAndMessages(null);
                    if (aMapLocation != null && aMapLocation.getAMapException().getErrorCode() == 0) {
                        if (mLocationListener != null) {
                            mLocationListener.onLocationChanged(aMapLocation);
                        }
                        if (listener != null) {
                            listener.onFindLocation(aMapLocation);
                        }
                    } else {
                        if (listener != null) {
                            listener.onFindLocationFail(aMapLocation.getAMapException().getErrorCode());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onFindLocationFail(-1);
            }
        }
    }

}
