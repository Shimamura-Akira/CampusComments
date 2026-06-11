package com.example.campuscomments;

public final class AppConstants {
    public static final String EXTRA_POI_OBJECT_ID = "poi_object_id";

    public static final String TYPE_BUILDING = "building";
    public static final String TYPE_CANTEEN = "canteen";
    public static final String TYPE_RESTAURANT = "restaurant";
    public static final String TYPE_STUDY_ROOM = "study_room";
    public static final String TYPE_DORM = "dorm";
    public static final String TYPE_SPORTS = "sports";
    public static final String TYPE_OTHER = "other";

    private AppConstants() {
    }

    public static String displayType(String type) {
        if (TYPE_BUILDING.equals(type)) return "教学楼";
        if (TYPE_CANTEEN.equals(type)) return "食堂";
        if (TYPE_RESTAURANT.equals(type)) return "餐厅";
        if (TYPE_STUDY_ROOM.equals(type)) return "自习室";
        if (TYPE_DORM.equals(type)) return "宿舍";
        if (TYPE_SPORTS.equals(type)) return "运动场";
        return "其他";
    }

    public static String typeFromPosition(int position) {
        switch (position) {
            case 0:
                return TYPE_BUILDING;
            case 1:
                return TYPE_CANTEEN;
            case 2:
                return TYPE_RESTAURANT;
            case 3:
                return TYPE_STUDY_ROOM;
            case 4:
                return TYPE_DORM;
            case 5:
                return TYPE_SPORTS;
            default:
                return TYPE_OTHER;
        }
    }

    public static int positionFromType(String type) {
        if (TYPE_BUILDING.equals(type)) return 0;
        if (TYPE_CANTEEN.equals(type)) return 1;
        if (TYPE_RESTAURANT.equals(type)) return 2;
        if (TYPE_STUDY_ROOM.equals(type)) return 3;
        if (TYPE_DORM.equals(type)) return 4;
        if (TYPE_SPORTS.equals(type)) return 5;
        return 6;
    }
}
