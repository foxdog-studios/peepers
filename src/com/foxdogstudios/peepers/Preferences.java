/* Copyright 2013 Foxdog Studios Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foxdogstudios.peepers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/* package */ final class Preferences
{
    private static final String HOST_NAME = "host_name";
    private static final String JPEG_QUALITY = "jpeg_quality";
    private static final String PORT = "port";

    private final SharedPreferences mPrefs;

    /**
     * This constuctor performs IO and so should not be called on the
     * main thread.
     */
    /* package */ Preferences(final Context context)
    {
        super();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    } // constructor()

    private int getInt(final String key, final int defValue)
    {
        try
        {
            return Integer.parseInt(mPrefs.getString(key, null /* defValue */));
        } //try
        catch (final NumberFormatException invalidValue)
        {
            return defValue;
        } // catch
    } // getInt(String, int)

    /* package */ int getJpegQuality()
    {
        // Default to the highest JPEG quality
        return getInt(JPEG_QUALITY, 40 /* defValue */);
    } // getJpegQuality()

    /* package */ int getPort()
    {
        return getInt(PORT, 8080 /* defValue */);
    } // getPort()


} // class Preferences

