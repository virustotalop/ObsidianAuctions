package me.virustotal.floauction.utility;

import java.util.ArrayList;
import java.util.List;

public class CArrayList<E> extends ArrayList<E>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 161221347262503383L;

	public CArrayList()
	{
		super();
	}
	
	public CArrayList(ArrayList<E> arr)
	{
		super();
		addAll(arr);
	}
	
	public CArrayList(E[]arr)
	{
		super();
		addAll(arr);
	}
	
	public CArrayList(List<E> list)
	{
		super();
		addAll(list);
	}
	
	public CArrayList(E e) 
	{
		this.add(e);
	}
	
	public void addAll(ArrayList<E> arr)
	{
		for(int i = 0; i < arr.size(); i++)
		{
			this.add(arr.get(i));
		}
	}
	
	public void addAll(E[]arr)
	{
		for(int i = 0; i < arr.length; i++)
		{
			this.add(arr[i]);
		}
	}
	
	public void addAll(List<E>list)
	{
		for(int i = 0; i < list.size(); i++)
		{
			this.add(list.get(i));
		}
	}
}