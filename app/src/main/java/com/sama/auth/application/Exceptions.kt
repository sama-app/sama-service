package com.sama.auth.application


class MissingScopesException :
    RuntimeException("User did not grant all required scopes")