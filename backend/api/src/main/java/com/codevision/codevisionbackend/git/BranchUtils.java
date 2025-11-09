package com.codevision.codevisionbackend.git;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BranchUtils {

    private static final String DEFAULT_BRANCH = "main";
    private static final String HEAD_PREFIX = "refs/heads/";

    public static String normalize(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            return DEFAULT_BRANCH;
        }
        String normalized = branchName.trim();
        if (normalized.startsWith(HEAD_PREFIX)) {
            normalized = normalized.substring(HEAD_PREFIX.length());
        }
        return normalized.isBlank() ? DEFAULT_BRANCH : normalized;
    }
}
