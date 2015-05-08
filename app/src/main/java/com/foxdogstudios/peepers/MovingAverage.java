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

/* package */ final class MovingAverage
{
    private final int mNumValues;
    private final long[] mValues;
    private int mEnd = 0;
    private int mLength = 0;
    private long mSum = 0L;

    /* package */ MovingAverage(final int numValues)
    {
        super();
        mNumValues = numValues;
        mValues = new long[numValues];
    } // constructor()

    /* package */ void update(final long value)
    {
        mSum -= mValues[mEnd];
        mValues[mEnd] = value;
        mEnd = (mEnd + 1) % mNumValues;
        if (mLength < mNumValues)
        {
            mLength++;
        } // if
        mSum += value;
    } // update(long)

    /* package */ double getAverage()
    {
        return mSum / (double) mLength;
    } // getAverage()

} // class MovingAverage

