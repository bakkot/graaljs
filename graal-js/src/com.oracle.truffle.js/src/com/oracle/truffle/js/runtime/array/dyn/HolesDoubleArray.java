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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetHoleCount;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetHoleCount;

import java.util.List;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class HolesDoubleArray extends AbstractContiguousDoubleArray {

    private static final HolesDoubleArray HOLES_DOUBLE_ARRAY = new HolesDoubleArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();
    public static final long HOLE_VALUE = 0x7ff8000000000001L; // NaN + 1
    public static final double HOLE_VALUE_DOUBLE = Double.longBitsToDouble(HOLE_VALUE);

    private HolesDoubleArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    public static HolesDoubleArray makeHolesDoubleArray(DynamicObject object, int length, double[] array, long indexOffset, int arrayOffset, int usedLength, int holeCount, int integrityLevel) {
        HolesDoubleArray arrayType = createHolesDoubleArray().setIntegrityLevel(integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        assert holeCount == arrayType.countHoles(object);
        return arrayType;
    }

    private static HolesDoubleArray createHolesDoubleArray() {
        return HOLES_DOUBLE_ARRAY;
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(DynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        return this;
    }

    @Override
    public int prepareInBounds(DynamicObject object, int index, ProfileHolder profile) {
        return prepareInBoundsHoles(object, index, profile);
    }

    @Override
    public void setInBoundsFast(DynamicObject object, int index, double value) {
        throw Errors.shouldNotReachHere("should not call this method, use setInBounds(Non)Hole");
    }

    public boolean isHoleFast(DynamicObject object, int index) {
        int internalIndex = (int) (index - getIndexOffset(object));
        return isHolePrepared(object, internalIndex);
    }

    public void setInBoundsFastHole(DynamicObject object, int index, double value) {
        int internalIndex = (int) (index - getIndexOffset(object));
        assert isHolePrepared(object, internalIndex);
        incrementHolesCount(object, -1);
        setInBoundsFastIntl(object, index, internalIndex, value);
    }

    public void setInBoundsFastNonHole(DynamicObject object, int index, double value) {
        int internalIndex = (int) (index - getIndexOffset(object));
        assert !isHolePrepared(object, internalIndex);
        setInBoundsFastIntl(object, index, internalIndex, value);
    }

    private void setInBoundsFastIntl(DynamicObject object, int index, int internalIndex, double value) {
        getArray(object)[internalIndex] = value;
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    public boolean containsHoles(DynamicObject object, long index) {
        return arrayGetHoleCount(object) > 0 || !isInBoundsFast(object, index);
    }

    @Override
    public AbstractDoubleArray toNonHoles(DynamicObject object, long index, Object value) {
        assert !containsHoles(object, index);
        double[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        int arrayOffset = getArrayOffset(object);
        long indexOffset = getIndexOffset(object);

        AbstractDoubleArray newArray;
        setInBoundsFastNonHole(object, (int) index, (double) value);
        if (indexOffset == 0 && arrayOffset == 0) {
            newArray = ZeroBasedDoubleArray.makeZeroBasedDoubleArray(object, length, usedLength, array, integrityLevel);
        } else {
            newArray = ContiguousDoubleArray.makeContiguousDoubleArray(object, length, array, indexOffset, arrayOffset, usedLength, integrityLevel);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    protected void incrementHolesCount(DynamicObject object, int offset) {
        arraySetHoleCount(object, arrayGetHoleCount(object) + offset);
    }

    @Override
    public boolean isSupported(DynamicObject object, long index) {
        return isSupportedHoles(object, index);
    }

    @Override
    public int prepareSupported(DynamicObject object, int index, ProfileHolder profile) {
        return prepareSupportedHoles(object, index, profile);
    }

    @Override
    public Object getInBoundsFast(DynamicObject object, int index) {
        double value = getInBoundsFastDouble(object, index);
        if (HolesDoubleArray.isHoleValue(value)) {
            return Undefined.instance;
        }
        return value;
    }

    @Override
    public HolesDoubleArray toHoles(DynamicObject object, long index, Object value) {
        return this;
    }

    @Override
    public AbstractWritableArray toObject(DynamicObject object, long index, Object value) {
        double[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        int arrayOffset = getArrayOffset(object);
        long indexOffset = getIndexOffset(object);
        int holeCount = arrayGetHoleCount(object);

        Object[] objectCopy = ArrayCopy.doubleToObjectHoles(array, arrayOffset, usedLength);
        HolesObjectArray newArray = HolesObjectArray.makeHolesObjectArray(object, length, objectCopy, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    public static boolean isHoleValue(double value) {
        return Double.doubleToRawLongBits(value) == HOLE_VALUE;
    }

    @Override
    public long nextElementIndex(DynamicObject object, long index0) {
        return nextElementIndexHoles(object, index0);
    }

    @Override
    public long previousElementIndex(DynamicObject object, long index0) {
        return previousElementIndexHoles(object, index0);
    }

    @Override
    public boolean hasElement(DynamicObject object, long index) {
        return super.hasElement(object, index) && !isHolePrepared(object, prepareInBoundsFast(object, (int) index));
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict) {
        return deleteElementHoles(object, index);
    }

    @Override
    public boolean isHolesType() {
        return true;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        return removeRangeHoles(object, start, end);
    }

    @Override
    protected HolesDoubleArray withIntegrityLevel(int newIntegrityLevel) {
        return new HolesDoubleArray(newIntegrityLevel, cache);
    }

    @Override
    public List<Object> ownPropertyKeys(DynamicObject object) {
        return ownPropertyKeysHoles(object);
    }
}
