package com.students42.app.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClientId

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClientSecret

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RedirectUri
