package com.alibaba.excel.metadata.property;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.converters.AutoConverter;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.HeadKindEnum;
import com.alibaba.excel.exception.ExcelCommonException;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.Holder;
import com.alibaba.excel.util.ClassUtils;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.excel.write.metadata.holder.AbstractWriteHolder;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define the header attribute of excel
 *
 * @author jipengfei
 */
@Data
public class ExcelHeadProperty {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelHeadProperty.class);
    /**
     * Custom class
     */
    private Class headClazz;
    /**
     * The types of head
     */
    private HeadKindEnum headKind;
    /**
     * The number of rows in the line with the most rows
     */
    private int headRowNumber;
    /**
     * Configuration header information
     */
    private Map<Integer, Head> headMap;
    /**
     * Configuration column information
     */
    private Map<Integer, ExcelContentProperty> contentPropertyMap;
    /**
     * Configuration column information
     */
    private Map<String, ExcelContentProperty> fieldNameContentPropertyMap;
    /**
     * Fields ignored
     */
    private Map<String, Field> ignoreMap;

    public ExcelHeadProperty(Holder holder, Class headClazz, List<List<String>> head) {
        this.headClazz = headClazz;
        headMap = new TreeMap<>();
        contentPropertyMap = new TreeMap<>();
        fieldNameContentPropertyMap = MapUtils.newHashMap();
        ignoreMap = MapUtils.newHashMap();
        headKind = HeadKindEnum.NONE;
        headRowNumber = 0;
        if (head != null && !head.isEmpty()) {
            int headIndex = 0;
            for (int i = 0; i < head.size(); i++) {
                if (holder instanceof AbstractWriteHolder) {
                    if (((AbstractWriteHolder)holder).ignore(null, i)) {
                        continue;
                    }
                }
                headMap.put(headIndex, new Head(headIndex, null, head.get(i), Boolean.FALSE, Boolean.TRUE));
                contentPropertyMap.put(headIndex, null);
                headIndex++;
            }
            headKind = HeadKindEnum.STRING;
        }
        // convert headClazz to head
        initColumnProperties(holder);

        initHeadRowNumber();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The initialization sheet/table 'ExcelHeadProperty' is complete , head kind is {}", headKind);
        }
    }

    private void initHeadRowNumber() {
        headRowNumber = 0;
        for (Head head : headMap.values()) {
            List<String> list = head.getHeadNameList();
            if (list != null && list.size() > headRowNumber) {
                headRowNumber = list.size();
            }
        }
        for (Head head : headMap.values()) {
            List<String> list = head.getHeadNameList();
            if (list != null && !list.isEmpty() && list.size() < headRowNumber) {
                int lack = headRowNumber - list.size();
                int last = list.size() - 1;
                for (int i = 0; i < lack; i++) {
                    list.add(list.get(last));
                }
            }
        }
    }

    private void initColumnProperties(Holder holder) {
        if (headClazz == null) {
            return;
        }
        // Declared fields
        Map<Integer, Field> sortedAllFiledMap = MapUtils.newTreeMap();
        Map<Integer, Field> indexFiledMap = MapUtils.newTreeMap();

        boolean needIgnore = (holder instanceof AbstractWriteHolder) && (
            !CollectionUtils.isEmpty(((AbstractWriteHolder)holder).getExcludeColumnFieldNames()) || !CollectionUtils
                .isEmpty(((AbstractWriteHolder)holder).getExcludeColumnIndexes()) || !CollectionUtils
                .isEmpty(((AbstractWriteHolder)holder).getIncludeColumnFieldNames()) || !CollectionUtils
                .isEmpty(((AbstractWriteHolder)holder).getIncludeColumnIndexes()));

        ClassUtils.declaredFields(headClazz, sortedAllFiledMap, indexFiledMap, ignoreMap, needIgnore, holder);

        for (Map.Entry<Integer, Field> entry : sortedAllFiledMap.entrySet()) {
            initOneColumnProperty(entry.getKey(), entry.getValue(), indexFiledMap.containsKey(entry.getKey()));
        }
        headKind = HeadKindEnum.CLASS;
    }

    /**
     * Initialization column property
     *
     * @param index
     * @param field
     * @param forceIndex
     * @return Ignore current field
     */
    private void initOneColumnProperty(int index, Field field, Boolean forceIndex) {
        ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
        List<String> tmpHeadList = new ArrayList<String>();
        boolean notForceName = excelProperty == null || excelProperty.value().length <= 0
            || (excelProperty.value().length == 1 && StringUtils.isEmpty((excelProperty.value())[0]));
        if (headMap.containsKey(index)) {
            tmpHeadList.addAll(headMap.get(index).getHeadNameList());
        } else {
            if (notForceName) {
                tmpHeadList.add(field.getName());
            } else {
                Collections.addAll(tmpHeadList, excelProperty.value());
            }
        }
        Head head = new Head(index, field.getName(), tmpHeadList, forceIndex, !notForceName);
        ExcelContentProperty excelContentProperty = new ExcelContentProperty();
        if (excelProperty != null) {
            Class<? extends Converter> convertClazz = excelProperty.converter();
            if (convertClazz != AutoConverter.class) {
                try {
                    Converter converter = convertClazz.newInstance();
                    excelContentProperty.setConverter(converter);
                } catch (Exception e) {
                    throw new ExcelCommonException("Can not instance custom converter:" + convertClazz.getName());
                }
            }
        }
        excelContentProperty.setHead(head);
        excelContentProperty.setField(field);
        excelContentProperty
            .setDateTimeFormatProperty(DateTimeFormatProperty.build(field.getAnnotation(DateTimeFormat.class)));
        excelContentProperty
            .setNumberFormatProperty(NumberFormatProperty.build(field.getAnnotation(NumberFormat.class)));
        headMap.put(index, head);
        contentPropertyMap.put(index, excelContentProperty);
        fieldNameContentPropertyMap.put(field.getName(), excelContentProperty);
    }

    public boolean hasHead() {
        return headKind != HeadKindEnum.NONE;
    }

}
