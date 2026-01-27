package com.sangita.grantha.backend.api.di

import com.sangita.grantha.backend.dal.SangitaDal
import org.koin.dsl.module

val dalModule = module {
    single { SangitaDal() }
}
