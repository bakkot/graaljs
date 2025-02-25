/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetUsedLength;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class ZeroBasedIntArray extends AbstractIntArray {

    private static final ZeroBasedIntArray ZERO_BASED_INT_ARRAY = new ZeroBasedIntArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();

    public static ZeroBasedIntArray makeZeroBasedIntArray(DynamicObject object, int length, int usedLength, int[] array, int integrityLevel) {
        ZeroBasedIntArray arrayType = createZeroBasedIntArray().setIntegrityLevel(integrityLevel);
        arraySetLength(object, length);
        arraySetUsedLength(object, usedLength);
        arraySetArray(object, array);
        return arrayType;
    }

    public static ZeroBasedIntArray createZeroBasedIntArray() {
        return ZERO_BASED_INT_ARRAY;
    }

    private ZeroBasedIntArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public boolean isSupported(DynamicObject object, long index) {
        return isSupportedZeroBased(object, (int) index);
    }

    @Override
    public int getInBoundsFastInt(DynamicObject object, int index) {
        return getArray(object)[index];
    }

    @Override
    public void setInBoundsFast(DynamicObject object, int index, int value) {
        getArray(object)[index] = value;
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    protected int prepareInBoundsFast(DynamicObject object, long index) {
        return (int) index;
    }

    @Override
    protected int prepareInBounds(DynamicObject object, int index, ProfileHolder profile) {
        prepareInBoundsZeroBased(object, index, profile);
        return index;
    }

    @Override
    protected int prepareSupported(DynamicObject object, int index, ProfileHolder profile) {
        prepareSupportedZeroBased(object, index, profile);
        return index;
    }

    @Override
    protected void setLengthLess(DynamicObject object, long length, ProfileHolder profile) {
        setLengthLessZeroBased(object, length, profile);
    }

    @Override
    public ZeroBasedDoubleArray toDouble(DynamicObject object, long index, double value) {
        int[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);

        double[] doubleCopy = ArrayCopy.intToDouble(array, 0, usedLength);
        ZeroBasedDoubleArray newArray = ZeroBasedDoubleArray.makeZeroBasedDoubleArray(object, length, usedLength, doubleCopy, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ZeroBasedObjectArray toObject(DynamicObject object, long index, Object value) {
        int[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);

        Object[] doubleCopy = ArrayCopy.intToObject(array, 0, usedLength);
        ZeroBasedObjectArray newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, length, usedLength, doubleCopy, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ContiguousIntArray toContiguous(DynamicObject object, long index, Object value) {
        int[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);

        ContiguousIntArray newArray = ContiguousIntArray.makeContiguousIntArray(object, length, array, 0, 0, usedLength, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray toHoles(DynamicObject object, long index, Object value) {
        int[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);

        AbstractWritableArray newArray;
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, containsHoleValue(object))) {
            newArray = toObjectHoles(object);
        } else {
            newArray = HolesIntArray.makeHolesIntArray(object, length, array, 0, 0, usedLength, 0, integrityLevel);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    protected HolesObjectArray toObjectHoles(DynamicObject object) {
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        return HolesObjectArray.makeHolesObjectArray(object, length, convertToObject(object), 0, 0, usedLength, 0, integrityLevel);
    }

    @Override
    public long firstElementIndex(DynamicObject object) {
        return 0;
    }

    @Override
    public long lastElementIndex(DynamicObject object) {
        return getUsedLength(object) - 1;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        int[] array = getArray(object);
        int usedLength = getUsedLength(object);
        long moveLength = usedLength - end;
        if (moveLength > 0) {
            System.arraycopy(array, (int) end, array, (int) start, (int) moveLength);
        }
        return this;
    }

    @Override
    public ScriptArray shiftRangeImpl(DynamicObject object, long from) {
        int usedLength = getUsedLength(object);
        if (from < usedLength) {
            return ContiguousIntArray.makeContiguousIntArray(object, lengthInt(object) - from, getArray(object), -from, (int) from, (int) (usedLength - from), integrityLevel);
        } else {
            return removeRangeImpl(object, 0, from);
        }
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        return addRangeImplZeroBased(object, offset, size);
    }

    @Override
    public boolean hasHoles(DynamicObject object) {
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        return usedLength < length;
    }

    @Override
    protected ZeroBasedIntArray withIntegrityLevel(int newIntegrityLevel) {
        return new ZeroBasedIntArray(newIntegrityLevel, cache);
    }

    @Override
    public long nextElementIndex(DynamicObject object, long index) {
        return nextElementIndexZeroBased(object, index);
    }
}
