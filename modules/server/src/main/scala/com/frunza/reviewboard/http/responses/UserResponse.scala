package com.frunza.reviewboard.http.responses

import zio.json.JsonCodec

final case class UserResponse(email: String) derives JsonCodec
