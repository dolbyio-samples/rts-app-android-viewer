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

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun Modifier.listItemHeight(): Modifier =
    this.heightIn(min = 60.dp)

fun Modifier.iconWidth(): Modifier =
    this.width(50.dp)
