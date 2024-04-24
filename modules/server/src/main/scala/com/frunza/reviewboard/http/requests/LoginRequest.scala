package com.frunza.reviewboard.http.requests

import zio.json.JsonCodec

case class LoginRequest(email: String, password: String) derives JsonCodec
