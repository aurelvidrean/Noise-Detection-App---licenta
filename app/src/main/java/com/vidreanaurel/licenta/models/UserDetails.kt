package com.vidreanaurel.licenta.models

data class UserDetails(
    val email: String?,
    val location: String?,
    val soundLevel: String?,
    val personDetected: String?,
    val carDetected: String?
) : Comparable<UserDetails> {
    override fun compareTo(other: UserDetails): Int {
        // If the emails are equal, the objects are the same
        if (email == other.email) {
            return 0
        }
        // If the emails are not equal, compare based on name
        return email!!.compareTo(other.email!!)
    }

}

