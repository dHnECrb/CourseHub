package com.example.springdemo.entity;

import org.jspecify.annotations.NonNull;

import java.time.LocalDate;

public class YearTerm {
    private final int year;
    private final int term;

    public YearTerm(int year, int term) {
        this.year = year;
        this.term = term;
    }

    public int getYear() {
        return year;
    }

    public int getTerm() {
        return term;
    }


    public boolean equals(@NonNull YearTerm yearTerm) {
        return yearTerm.getTerm()==term && yearTerm.getYear()==year;
    }


    public boolean isBefore(@NonNull YearTerm yearTerm){
        if(year!=yearTerm.getYear()){
            return year<yearTerm.getYear();
        }
        return term<yearTerm.getTerm();
    }

    public boolean isAfter(@NonNull YearTerm yearTerm){
        if(year!=yearTerm.getYear()){
            return year>yearTerm.getYear();
        }
        return term>yearTerm.getTerm();
    }

    public String toString(){
        return year+"-"+(year+1)+"-"+term;
    }

    public static YearTerm parse(@NonNull String string){
        String[] strings=string.split("-");
        int year1,year2,term;
        if (strings.length!=3){
            throw new RuntimeException("无法转化：格式错误");
        }
        year1=Integer.parseInt(strings[0]);
        year2=Integer.parseInt(strings[1]);
        term=Integer.parseInt(strings[2]);
        if(year2-year1!=1){
            throw new RuntimeException("无法转化：学年中，两年差值错误");
        }
        if(term!=1 && term!=2){
            throw new RuntimeException("无法转化：学期错误");
        }
        return new YearTerm(year1, term);
    }
    public static YearTerm parse(@NonNull LocalDate localDate){
        // 9~12月：year-(year+1)-1
        // 1~2月：(year-1)-year-1
        // 3~8月：(year-1)-year-2
        int year=localDate.getYear();
        int month=localDate.getMonthValue();
        if(month>=9){
            return new YearTerm(year, 1);
        }
        if(month<=2){
            return new YearTerm(year-1, 1);
        }
        return new YearTerm(year-1, 2);
    }

    public static YearTerm now(){
        return YearTerm.parse(LocalDate.now());
    }

}
