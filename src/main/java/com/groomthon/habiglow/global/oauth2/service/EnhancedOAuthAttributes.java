package com.groomthon.habiglow.global.oauth2.service;

import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;

public class EnhancedOAuthAttributes {
    
    private final OAuthAttributes original;
    private final SocialType socialType;
    
    public EnhancedOAuthAttributes(OAuthAttributes original, SocialType socialType) {
        this.original = original;
        this.socialType = socialType;
    }
    
    public String getEmail() {
        return original.getEmail();
    }
    
    public String getName() {
        return original.getName();
    }
    
    public SocialType getSocialType() {
        return this.socialType;
    }
    
    public String getSocialUniqueId() {
        return this.socialType.name() + "_" + original.getSocialUniqueId();
    }
}