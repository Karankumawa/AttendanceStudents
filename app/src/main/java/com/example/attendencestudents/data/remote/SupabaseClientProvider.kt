package com.example.attendencestudents.data.remote

object SupabaseClientProvider {
    const val SUPABASE_URL = "https://znncfjsmzvldlhedajbw.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_fxsPUBUp0ZomNFafRDStDA_ldJUbgPn"
    const val SECRET_KEY = ""

    val client: SupabaseClient by lazy {
        SupabaseClient()
    }
}
