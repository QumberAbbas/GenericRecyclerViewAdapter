package com.zeyad.gadapter.di

import android.content.Context
import android.util.Log
import com.zeyad.gadapter.BuildConfig
import com.zeyad.gadapter.screens.user.detail.UserDetailVM
import com.zeyad.gadapter.screens.user.list.UserListVM
import com.zeyad.gadapter.utils.Constants.URLS.API_BASE_URL
import com.zeyad.usecases.api.DataServiceConfig
import com.zeyad.usecases.api.DataServiceFactory
import com.zeyad.usecases.api.IDataService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import java.util.concurrent.TimeUnit

val myModule: Module = module {
    viewModel { UserListVM(get()) }
    viewModel { UserDetailVM(get()) }

    single { createDataService(get()) }
}

fun createDataService(context: Context): IDataService {
    DataServiceFactory(DataServiceConfig.Builder(context)
            .baseUrl(API_BASE_URL)
            .okHttpBuilder(getOkHttpBuilder())
            .withRealm()
            .build())
    return DataServiceFactory.dataService!!
}

fun getOkHttpBuilder(): OkHttpClient.Builder {
    return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor { Log.d("NetworkInfo", it) }
                    .setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE))
            .connectTimeout(15L, TimeUnit.SECONDS)
            .writeTimeout(15L, TimeUnit.SECONDS)
            .readTimeout(15L, TimeUnit.SECONDS)
//            .certificatePinner(CertificatePinner.Builder()
//                    .add(API_BASE_URL, "sha256/6wJsqVDF8K19zxfLxV5DGRneLyzso9adVdUN/exDacw")
//                    .add(API_BASE_URL, "sha256/k2v657xBsOVe1PQRwOsHsw3bsGT2VzIqz5K+59sNQws=")
//                    .add(API_BASE_URL, "sha256/WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=").build())
//            .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
}
