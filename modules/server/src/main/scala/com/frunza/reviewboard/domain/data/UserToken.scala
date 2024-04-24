package com.frunza.reviewboard.domain.data

import zio.json.JsonCodec

final case class UserToken(
    email: String,
    token: String,
    expires: Long
) derives JsonCodec
