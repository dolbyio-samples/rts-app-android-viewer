/*
 *
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                   Copyright (C) 2022 by Dolby Laboratories.
 *                              All rights reserved.
 *
 */

package io.dolby.rtsviewer.uikit.utils

/**
 * An analogous enum to the traditional Android view state model where in the state of a view can be
 * determined. For those familiar with the traditional android view of having a view state selector for
 * applying drawables or colors, this should feel like the jetpack compose equivalent.
 */
enum class ViewState {
    Unknown,
    Pressed,
    Selected,
    Disabled;

    companion object Factory {
        fun from(isPressed: Boolean, isSelected: Boolean, isEnabled: Boolean): ViewState {
            return if (isPressed) {
                Pressed
            } else if (isSelected) {
                Selected
            } else if (!isEnabled) {
                Disabled
            } else Unknown
        }
    }
}
