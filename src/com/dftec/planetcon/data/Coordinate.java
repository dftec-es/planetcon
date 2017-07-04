/*
 * Copyright 2014 David Fernandez <dftec.es@gmail.com>
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
package com.dftec.planetcon.data;

public class Coordinate {
    public int i;
    public int j;

    public Coordinate() {
        i = 0;
        j = 0;
    }

    public Coordinate(int x, int y) {
        i = x;
        j = y;
    }

    public Coordinate(int r, int a, int maxRadius) {
        // convert r=radius (of the orbit), a=arc (measured as tiles around the
        // orbit) to coordinates i,j of the matrix Grid.

        // r from =0 to <=maxRadius
        if (r <= 0) {
            i = maxRadius;
            j = maxRadius;
            return;
        }
        if (r > maxRadius) {
            r = maxRadius;
        }

        // a from =0 to <8*r
        a = a % (8*r);
        if (a < 0) {
            a = a + 8*r;
        }

        if (a <= r) {
            i = r;
            j = a;
        } else if ((a > r) && (a <= 3*r)) {
            i = 2*r - a;
            j = r;
        } else if ((a > 3*r) && (a <= 5*r)) {
            i = -r;
            j = 4*r - a;
        } else if ((a > 5*r) && (a <= 7*r)) {
            i = a - 6*r;
            j = -r;
        } else { //if (a > 7*r)
            i = r;
            j = a - 8*r;
        }
        // coordinates i,j from =0 to <=(2*maxRadius)
        i = i + maxRadius;
        j = j + maxRadius;
    }
}
