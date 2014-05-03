package com.example.controldemo;

import java.util.ArrayList;
import java.util.ListIterator;

import android.graphics.Point;

public class PointList {
	
	private ArrayList<Point> pointList;
	 
    public PointList() {
        pointList = new ArrayList<Point>();
    }
    
    public void add(Point point) {
        pointList.add(point);
    }
    
    public void add(int index, Point point) {
    	pointList.add(index, point);
    }
    
    public void remove(int pointID){
    	pointList.remove(pointID);
    }
    
    public void set(int id,Point point){
    	pointList.set(id, point);
    }
    
    public Point get(int id){
    	return pointList.get(id);
    }
    
    public int size(){
    	return pointList.size();
    }
    
    public ListIterator<Point> pointListIterator() {
    	return pointList.listIterator();
    }
}
