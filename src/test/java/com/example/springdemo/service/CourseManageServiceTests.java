package com.example.springdemo.service;

import com.example.springdemo.dto.ManagedCourseView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.Course;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.mapper.CourseMapper;
import com.example.springdemo.mapper.CourseSelectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseManageServiceTests {

    @Mock
    private MessageSource msgSource;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseSelectMapper courseSelectMapper;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private CourseManageService courseManageService;

    @BeforeEach
    void setUp() {
        lenient().when(httpSession.getAttribute("userId")).thenReturn("12");
        lenient().when(httpSession.getAttribute("userPosition")).thenReturn(UserPosition.ADMIN);
        lenient().when(msgSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsNotLoggedIn() {
        when(httpSession.getAttribute("userId")).thenReturn(null);

        RequestResult result = courseManageService.index(httpSession);

        assertFalse(result.isSucceed());
        assertEquals(401, result.code());
    }

    @Test
    void rejectsNonAdmin() {
        when(httpSession.getAttribute("userPosition")).thenReturn(UserPosition.STUDENT);

        RequestResult result = courseManageService.index(httpSession);

        assertFalse(result.isSucceed());
        assertEquals(403, result.code());
    }

    @Test
    void acceptsAdmin() {
        RequestResult result = courseManageService.index(httpSession);

        assertTrue(result.isSucceed());
        assertEquals(200, result.code());
    }

    @Test
    void rejectsIllegalCourseFields() {
        List<ManagedCourseView> courses = List.of(
                course(0L, "课程", "2026-2027-1", "选修", true, 3),
                course(1L, " ", "2026-2027-1", "选修", true, 3),
                course(2L, "课程", "2026-2027-1", " ", true, 3),
                course(3L, "课程", "2026-2027-1", "选修", null, 3),
                course(4L, "课程", "2026-2027-1", "选修", true, 0),
                course(5L, "课程", "2026-2026-1", "选修", true, 3),
                course(6L, "课程", "2026-2027-3", "选修", true, 3)
        );

        RequestResult result = courseManageService.createCourses(httpSession, courses);

        assertFalse(result.isSucceed());
        verifyNoInteractions(courseMapper, courseSelectMapper);
    }

    @Test
    void createsValidRowsAndRejectsDuplicateIds() {
        ManagedCourseView course = course(100L, "数据库", "2026-2027-1", "专业选修课", true, 3);
        when(courseMapper.selectById(100L)).thenReturn(null);

        RequestResult result = courseManageService.createCourses(httpSession, List.of(course, course));

        assertFalse(result.isSucceed());
        verify(courseMapper).insert(any(Course.class));
    }

    @Test
    void updatesExistingRowsAndSkipsMissingCourses() {
        ManagedCourseView existing = course(100L, "数据库", "2026-2027-1", "专业选修课", true, 3);
        ManagedCourseView missing = course(101L, "操作系统", "2026-2027-1", "专业选修课", true, 4);
        when(courseMapper.selectById(100L)).thenReturn(new Course(100L));
        when(courseMapper.selectById(101L)).thenReturn(null);

        RequestResult result = courseManageService.updateCourses(httpSession, List.of(existing, missing));

        assertFalse(result.isSucceed());
        verify(courseMapper).updateById(any(Course.class));
        verify(courseMapper, never()).deleteById(101L);
    }

    @Test
    void refusesToDeleteCourseWithSelections() {
        when(courseMapper.selectById(100L)).thenReturn(new Course(100L));
        when(courseSelectMapper.selectCount(any())).thenReturn(1L);

        RequestResult result = courseManageService.deleteCourses(httpSession, List.of(100L));

        assertFalse(result.isSucceed());
        verify(courseMapper, never()).deleteById(100L);
    }

    @Test
    void deletesCourseWithoutSelections() {
        when(courseMapper.selectById(100L)).thenReturn(new Course(100L));
        when(courseSelectMapper.selectCount(any())).thenReturn(0L);

        RequestResult result = courseManageService.deleteCourses(httpSession, List.of(100L));

        assertTrue(result.isSucceed());
        verify(courseMapper).deleteById(100L);
    }

    private static ManagedCourseView course(Long id, String name, String term, String property,
                                            Boolean willOffer, Integer credit) {
        ManagedCourseView course = new ManagedCourseView();
        course.setId(id);
        course.setName(name);
        course.setTerm(term);
        course.setProperty(property);
        course.setWill_offer(willOffer);
        course.setCredit(credit);
        return course;
    }
}
