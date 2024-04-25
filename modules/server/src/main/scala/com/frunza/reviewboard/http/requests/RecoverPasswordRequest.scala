package com.frunza.reviewboard.http.requests

import zio.json.JsonCodec


case class RecoverPasswordRequest(email: String, token: String, newPassword: String) derives JsonCodec