package pushservice.utils;

import java.beans.PropertyDescriptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import pushservice.Pojo.StringValue;

public class CommonUtils {

public static Path pathGet(String first, String... more) {
		
		String first1 = clone(first);
		String[] more1 = Arrays.asList(more).stream().map(CommonUtils::clone).toArray(String[]::new);
		return Paths.get(first1, more1);
	}
	
	public static String clone(String value) {
		StringValue v1 = new StringValue();
		StringValue v2 = new StringValue();
		v1.setValue(value);;
		copyPropertiesWithoutNull(v1, v2);
		return v2.getValue();
	}
	
	public static <T> T clone(T value, Class<T> tclass) throws InstantiationException, IllegalAccessException {
		T dest = tclass.newInstance();
		copyPropertiesWithoutNull(value, dest);
		return dest;
	}
	
	public static <T> Page<T> clonePage(Page<T> value, Class<T> tclass) throws InstantiationException, IllegalAccessException {
		return new PageImpl<>(cloneList(value.getContent(), tclass), value.getPageable(), value.getTotalElements());
	}

	public static <T> List<T> cloneList(List<T> source, Class<T> tclass) throws InstantiationException, IllegalAccessException {
		List<T> dest = new ArrayList<T>();
		for (T s : source) {
			dest.add(clone(s, tclass));
		}
		return dest;
	}
	
	public static void copyPropertiesWithoutNull(Object source, Object target) {
		BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
	}
	
	public static <T> List<T> copyPropertiesWithoutNull(List<T> source) {
		if (source.size() == 0)
			return new ArrayList<T>();
		
		return null;
	}
	
	/**
	 * 获取需要忽略的属性
	 * 
	 * @param source
	 * @return
	 */
	public static String[] getNullPropertyNames (Object source) {
	    final BeanWrapper src = new BeanWrapperImpl(source);
	    PropertyDescriptor[] pds = src.getPropertyDescriptors();
	 
	    Set<String> emptyNames = new HashSet<>();
	    for(PropertyDescriptor pd : pds) {
	    	Object srcValue = null;
	    	try {
	    		srcValue = src.getPropertyValue(pd.getName());
	    	} catch (InvalidPropertyException ex) {
	    		srcValue = null;
	    	}
	        // 此处判断可根据需求修改
	        if (srcValue == null) {
	            emptyNames.add(pd.getName());
	        }
	    }
	    String[] result = new String[emptyNames.size()];
	    return emptyNames.toArray(result);
	}
	
	public static List<Object> cloneListWithoutNull(List<Object> source) {
		List<Object> result = new ArrayList<Object>();
		
		source.forEach(o -> {
			Object n;
			try {
				n = o.getClass().newInstance();
				copyPropertiesWithoutNull(o, n);
			} catch (InstantiationException | IllegalAccessException e) {
				n = null;
			}
			if (n != null) {
				result.add(n);				
			}
		});
		return result;
	}
}
