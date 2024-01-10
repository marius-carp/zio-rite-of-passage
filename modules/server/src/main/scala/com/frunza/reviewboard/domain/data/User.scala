package com.frunza.reviewboard.domain.data

final case class User (
    id: Long,
    email: String,
    hashedPassword: String
)
