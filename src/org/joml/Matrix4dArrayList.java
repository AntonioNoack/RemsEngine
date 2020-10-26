package org.joml;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

/**
 * A stack of many {@link Matrix4f} instances. This resembles the matrix stack known from legacy OpenGL.
 * <p>
 * This {@link Matrix4dArrayList} class inherits from {@link Matrix4f}, so the current/top matrix is always the {@link Matrix4dArrayList}/{@link Matrix4f} itself. This
 * affects all operations in {@link Matrix4f} that take another {@link Matrix4f} as parameter. If a {@link Matrix4dArrayList} is used as argument to those methods,
 * the effective argument will always be the <i>current</i> matrix of the matrix stack.
 * 
 * @author Kai Burjack, modified by Antonio Noack to be infinite, yet not allocation free
 * users of RemsStudio shouldn't have to worry about tree depth, if their machine is strong enough to handle it
 */
public class Matrix4dArrayList extends Matrix4d {

    private static final long serialVersionUID = 1L;

    /**
     * The matrix stack
     */
    private ArrayList<Matrix4d> matrices = new ArrayList<>();

    /**
     * The index of the "current" matrix within {@link #matrices}.
     */
    private int currentIndex;

    public Matrix4dArrayList() {}

    public int getSize(){
        return matrices.size();
    }
    public int getCurrentIndex() { return currentIndex; }

    /**
     * Set the stack pointer to zero and set the current/bottom matrix to {@link #identity() identity}.
     * 
     * @return this
     */
    public Matrix4dArrayList clear() {
        currentIndex = 0;
        identity();
        return this;
    }

    /**
     * Increment the stack pointer by one and set the values of the new current matrix to the one directly below it.
     * 
     * @return this
     */
    public Matrix4dArrayList pushMatrix() {
        if (currentIndex == matrices.size()) {
            matrices.add(new Matrix4d());
        }
        matrices.get(currentIndex++).set(this);
        return this;
    }

    /**
     * Decrement the stack pointer by one.
     * <p>
     * This will effectively dispose of the current matrix.
     * 
     * @return this
     */
    public Matrix4dArrayList popMatrix() {
        if (currentIndex == 0) {
            throw new IllegalStateException("already at the buttom of the stack"); //$NON-NLS-1$
        }
        set(matrices.get(--currentIndex));
        return this;
    }

    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + currentIndex;
        for (int i = 0; i < currentIndex; i++) {
            result = prime * result + matrices.get(i).hashCode();
        }
        return result;
    }

    /*
     * Contract between Matrix4f and Matrix4fStack:
     * 
     * - Matrix4f.equals(Matrix4fStack) is true iff all the 16 matrix elements are equal
     * - Matrix4fStack.equals(Matrix4f) is true iff all the 16 matrix elements are equal
     * - Matrix4fStack.equals(Matrix4fStack) is true iff all 16 matrix elements are equal AND the matrix arrays as well as the stack pointer are equal
     * - everything else is inequal
     * 
     * (non-Javadoc)
     * @see org.joml.Matrix4f#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (obj instanceof Matrix4dArrayList) {
            Matrix4dArrayList other = (Matrix4dArrayList) obj;
            if (currentIndex != other.currentIndex)
                return false;
            for (int i = 0; i < currentIndex; i++) {
                if (!matrices.get(i).equals(other.matrices.get(i)))
                    return false;
            }
        }
        return true;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(currentIndex);
        for (int i = 0; i < currentIndex; i++) {
            out.writeObject(matrices.get(i));
        }
    }

    public void readExternal(ObjectInput in) throws IOException {
        super.readExternal(in);
        currentIndex = in.readInt();
        matrices = new ArrayList<>(currentIndex);
        for (int i = 0; i < currentIndex; i++) {
            Matrix4d m = new Matrix4d();
            m.readExternal(in);
            matrices.set(i, m);
        }
    }

}
