package com.alibaba.excel.converters.bigdecimal;

import java.math.BigDecimal;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.ExcelColumnProperty;

/**
 * BigDecimal and boolean converter
 *
 * @author zhuangjiaju
 */
public class BigDecimalBooleanConverter implements Converter<BigDecimal> {

    @Override
    public Class supportJavaTypeKey() {
        return BigDecimal.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.BOOLEAN;
    }

    @Override
    public BigDecimal convertToJavaData(CellData cellData, ExcelColumnProperty columnProperty) {
        if (cellData.getBooleanValue()) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ZERO;
    }

    @Override
    public CellData convertToExcelData(BigDecimal value, ExcelColumnProperty columnProperty) {
        if (BigDecimal.ONE.equals(value)) {
            return new CellData(Boolean.TRUE);
        }
        return new CellData(Boolean.FALSE);
    }
}