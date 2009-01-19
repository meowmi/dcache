/**
 * ArrayOfTSURLLifetimeReturnStatus.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.dcache.srm.v2_2;

public class ArrayOfTSURLLifetimeReturnStatus  implements java.io.Serializable {
    private org.dcache.srm.v2_2.TSURLLifetimeReturnStatus[] statusArray;

    public ArrayOfTSURLLifetimeReturnStatus() {
    }

    public ArrayOfTSURLLifetimeReturnStatus(
           org.dcache.srm.v2_2.TSURLLifetimeReturnStatus[] statusArray) {
           this.statusArray = statusArray;
    }


    /**
     * Gets the statusArray value for this ArrayOfTSURLLifetimeReturnStatus.
     * 
     * @return statusArray
     */
    public org.dcache.srm.v2_2.TSURLLifetimeReturnStatus[] getStatusArray() {
        return statusArray;
    }


    /**
     * Sets the statusArray value for this ArrayOfTSURLLifetimeReturnStatus.
     * 
     * @param statusArray
     */
    public void setStatusArray(org.dcache.srm.v2_2.TSURLLifetimeReturnStatus[] statusArray) {
        this.statusArray = statusArray;
    }

    public org.dcache.srm.v2_2.TSURLLifetimeReturnStatus getStatusArray(int i) {
        return this.statusArray[i];
    }

    public void setStatusArray(int i, org.dcache.srm.v2_2.TSURLLifetimeReturnStatus _value) {
        this.statusArray[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ArrayOfTSURLLifetimeReturnStatus)) return false;
        ArrayOfTSURLLifetimeReturnStatus other = (ArrayOfTSURLLifetimeReturnStatus) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.statusArray==null && other.getStatusArray()==null) || 
             (this.statusArray!=null &&
              java.util.Arrays.equals(this.statusArray, other.getStatusArray())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getStatusArray() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getStatusArray());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getStatusArray(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ArrayOfTSURLLifetimeReturnStatus.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://srm.lbl.gov/StorageResourceManager", "ArrayOfTSURLLifetimeReturnStatus"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("statusArray");
        elemField.setXmlName(new javax.xml.namespace.QName("", "statusArray"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://srm.lbl.gov/StorageResourceManager", "TSURLLifetimeReturnStatus"));
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
