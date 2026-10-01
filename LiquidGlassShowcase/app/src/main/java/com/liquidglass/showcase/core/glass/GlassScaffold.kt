/*
 * Copyright 2025 Kyant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liquidglass.showcase.core.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liquidglass.showcase.R

/**
 * 1:1 port of the upstream demo scaffold
 * (`app/src/androidMain/kotlin/com/kyant/backdrop/catalog/BackdropDemoScaffold.kt`,
 * image picker removed).
 *
 * The wallpaper is recorded into a [LayerBackdrop]; every Liquid Glass component of the app
 * refracts that very layer, which is what keeps the material coherent across the whole UI —
 * including during the splash → catalog transition.
 */
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val backdrop = rememberLayerBackdrop()

        Image(
            painter = painterResource(R.drawable.wallpaper_light),
            contentDescription = null,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .then(modifier)
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        content(backdrop)
    }
}