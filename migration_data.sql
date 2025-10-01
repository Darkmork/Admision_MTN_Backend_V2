--
-- PostgreSQL database dump
--

\restrict MmgIiHZMzFGyZDvYnsdsPyF4mjxPa91qAviLmcGGBbPRpgDTh4zKJljJDdydTtS

-- Dumped from database version 16.9 (Debian 16.9-1.pgdg120+1)
-- Dumped by pg_dump version 16.10 (Debian 16.10-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS fktr3t9onllcgbtsocwyqf7c1ay;
ALTER TABLE IF EXISTS ONLY public.evaluation_schedules DROP CONSTRAINT IF EXISTS fksw5iqjypi6m4tf2xgvo9xnvm7;
ALTER TABLE IF EXISTS ONLY public.professor_subjects DROP CONSTRAINT IF EXISTS fksjxqv931ljk4uuof4ob6vopga;
ALTER TABLE IF EXISTS ONLY public.evaluations DROP CONSTRAINT IF EXISTS fkqihdmjba0yaamhjp8gr00c27m;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS fknh2osxuurfe1dtopyyxi9j4k0;
ALTER TABLE IF EXISTS ONLY public.kinder_teacher_specializations DROP CONSTRAINT IF EXISTS fkmd9w3lglly5vh5iyd8cdgjnvb;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS fkm6w0opoixba8hisj51yapw2yw;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS fkm3ipmdilwyvhhaq4jaim7pvun;
ALTER TABLE IF EXISTS ONLY public.progreso_usuario DROP CONSTRAINT IF EXISTS fkk8nu4vuk3au5efqu76gtapa2j;
ALTER TABLE IF EXISTS ONLY public.support_staff_responsibilities DROP CONSTRAINT IF EXISTS fkjqrnfl5y2uo5pjbi1t2klmt2s;
ALTER TABLE IF EXISTS ONLY public.support_staff DROP CONSTRAINT IF EXISTS fki4tt3e4g95rwp2p81h8286r26;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS fkhobxf5y55guo775hl1agtlvmr;
ALTER TABLE IF EXISTS ONLY public.professors DROP CONSTRAINT IF EXISTS fkfvoolphnt6fjvmat24p26u57m;
ALTER TABLE IF EXISTS ONLY public.evaluation_schedules DROP CONSTRAINT IF EXISTS fkf905brl97n1s0uyjvmoqsry4g;
ALTER TABLE IF EXISTS ONLY public.kinder_teachers DROP CONSTRAINT IF EXISTS fkex1e8gqykdk8vljxhhd504n8f;
ALTER TABLE IF EXISTS ONLY public.psychologists DROP CONSTRAINT IF EXISTS fke9xl394g2y647yscq6tdlut4;
ALTER TABLE IF EXISTS ONLY public.ranking DROP CONSTRAINT IF EXISTS fkdk59vfgtvw0qr12a4y9fh5xwn;
ALTER TABLE IF EXISTS ONLY public.professor_grades DROP CONSTRAINT IF EXISTS fkcyeelpt968uweivesr2923vgj;
ALTER TABLE IF EXISTS ONLY public.evaluations DROP CONSTRAINT IF EXISTS fkct8v0spukoo68fa2p9suju4nc;
ALTER TABLE IF EXISTS ONLY public.psychologist_specialized_areas DROP CONSTRAINT IF EXISTS fkclqeyjswsjmfgow2qvcaybq7r;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS fkbxjuiec753shgoyw6x0l8opn8;
ALTER TABLE IF EXISTS ONLY public.interviews DROP CONSTRAINT IF EXISTS fk_interview_interviewer;
ALTER TABLE IF EXISTS ONLY public.interviews DROP CONSTRAINT IF EXISTS fk_interview_application;
ALTER TABLE IF EXISTS ONLY public.kinder_teacher_qualifications DROP CONSTRAINT IF EXISTS fk9fa8xuxbcpjkxxql1r450c6xn;
ALTER TABLE IF EXISTS ONLY public.evaluation_schedules DROP CONSTRAINT IF EXISTS fk929j8xym0wlneuonx5ey3twxf;
ALTER TABLE IF EXISTS ONLY public.documents DROP CONSTRAINT IF EXISTS fk8umh06sslm8f0rbfasqk6yy0f;
ALTER TABLE IF EXISTS ONLY public.problemas DROP CONSTRAINT IF EXISTS fk734g8qk965lgfaawmemsx7quq;
ALTER TABLE IF EXISTS ONLY public.professor_qualifications DROP CONSTRAINT IF EXISTS fk5b7qr0spgjspxc0ht26i67omm;
ALTER TABLE IF EXISTS ONLY public.evaluations DROP CONSTRAINT IF EXISTS fk4dmy35k49uvtqh1r5mu6hu9t7;
ALTER TABLE IF EXISTS ONLY public.psychologist_grades DROP CONSTRAINT IF EXISTS fk4crcq1yuj1095h0plv0jx254q;
ALTER TABLE IF EXISTS ONLY public.progreso_usuario DROP CONSTRAINT IF EXISTS fk31f8nnpf2w40at3hwrljvcurw;
ALTER TABLE IF EXISTS ONLY public.email_notifications DROP CONSTRAINT IF EXISTS email_notifications_interview_id_fkey;
ALTER TABLE IF EXISTS ONLY public.email_notifications DROP CONSTRAINT IF EXISTS email_notifications_application_id_fkey;
ALTER TABLE IF EXISTS ONLY public.email_events DROP CONSTRAINT IF EXISTS email_events_email_notification_id_fkey;
DROP TRIGGER IF EXISTS trigger_validate_completed_interview ON public.interviews;
DROP TRIGGER IF EXISTS trigger_update_interviews_updated_at ON public.interviews;
DROP TRIGGER IF EXISTS trigger_email_notifications_updated_at ON public.email_notifications;
DROP INDEX IF EXISTS public.idx_interviews_type;
DROP INDEX IF EXISTS public.idx_interviews_status;
DROP INDEX IF EXISTS public.idx_interviews_scheduled_date;
DROP INDEX IF EXISTS public.idx_interviews_result;
DROP INDEX IF EXISTS public.idx_interviews_mode;
DROP INDEX IF EXISTS public.idx_interviews_interviewer_id;
DROP INDEX IF EXISTS public.idx_interviews_interviewer_date;
DROP INDEX IF EXISTS public.idx_interviews_follow_up;
DROP INDEX IF EXISTS public.idx_interviews_date_status;
DROP INDEX IF EXISTS public.idx_interviews_completed_at;
DROP INDEX IF EXISTS public.idx_interviews_application_id;
DROP INDEX IF EXISTS public.idx_email_notifications_type;
DROP INDEX IF EXISTS public.idx_email_notifications_tracking_token;
DROP INDEX IF EXISTS public.idx_email_notifications_sent_at;
DROP INDEX IF EXISTS public.idx_email_notifications_response_token;
DROP INDEX IF EXISTS public.idx_email_notifications_application;
DROP INDEX IF EXISTS public.idx_email_events_type;
DROP INDEX IF EXISTS public.idx_email_events_notification;
DROP INDEX IF EXISTS public.idx_email_events_created_at;
ALTER TABLE IF EXISTS ONLY public.usuarios DROP CONSTRAINT IF EXISTS usuarios_pkey;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY public.interviews DROP CONSTRAINT IF EXISTS unique_interviewer_datetime;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS ukscuj1snh0iy35s195t3qff5o;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS uknj2nqjrdbo9g3ywd8mhf934tx;
ALTER TABLE IF EXISTS ONLY public.usuarios DROP CONSTRAINT IF EXISTS ukm2dvbwfge291euvmk6vkkocao;
ALTER TABLE IF EXISTS ONLY public.usuarios DROP CONSTRAINT IF EXISTS ukkfsp0s1tflm1cwlj8idhqsad0;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS ukgwerd1mccqjwm1loopov582bp;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS ukg3exv20ni4ytaxyabji4f087i;
ALTER TABLE IF EXISTS ONLY public.email_verification_tokens DROP CONSTRAINT IF EXISTS ukewmvysc7e9y6uy7og2c21axa9;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS ukbjc0uvubm2oywqk7gpgdqigt1;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS uk8yxcoymjxse3o3v2blmj3elya;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS uk6dotkott2kjsp8vw4d0m25fb7;
ALTER TABLE IF EXISTS ONLY public.ranking DROP CONSTRAINT IF EXISTS uk25758bs2emn7kip8o039qp3jn;
ALTER TABLE IF EXISTS ONLY public.temas DROP CONSTRAINT IF EXISTS temas_pkey;
ALTER TABLE IF EXISTS ONLY public.supporters DROP CONSTRAINT IF EXISTS supporters_pkey;
ALTER TABLE IF EXISTS ONLY public.support_staff DROP CONSTRAINT IF EXISTS support_staff_pkey;
ALTER TABLE IF EXISTS ONLY public.students DROP CONSTRAINT IF EXISTS students_rut_key;
ALTER TABLE IF EXISTS ONLY public.students DROP CONSTRAINT IF EXISTS students_pkey;
ALTER TABLE IF EXISTS ONLY public.ranking DROP CONSTRAINT IF EXISTS ranking_pkey;
ALTER TABLE IF EXISTS ONLY public.psychologists DROP CONSTRAINT IF EXISTS psychologists_pkey;
ALTER TABLE IF EXISTS ONLY public.progreso_usuario DROP CONSTRAINT IF EXISTS progreso_usuario_pkey;
ALTER TABLE IF EXISTS ONLY public.professors DROP CONSTRAINT IF EXISTS professors_pkey;
ALTER TABLE IF EXISTS ONLY public.problemas DROP CONSTRAINT IF EXISTS problemas_pkey;
ALTER TABLE IF EXISTS ONLY public.parents DROP CONSTRAINT IF EXISTS parents_pkey;
ALTER TABLE IF EXISTS ONLY public.kinder_teachers DROP CONSTRAINT IF EXISTS kinder_teachers_pkey;
ALTER TABLE IF EXISTS ONLY public.interviews DROP CONSTRAINT IF EXISTS interviews_pkey;
ALTER TABLE IF EXISTS ONLY public.guardians DROP CONSTRAINT IF EXISTS guardians_pkey;
ALTER TABLE IF EXISTS ONLY public.evaluations DROP CONSTRAINT IF EXISTS evaluations_pkey;
ALTER TABLE IF EXISTS ONLY public.evaluation_schedules DROP CONSTRAINT IF EXISTS evaluation_schedules_pkey;
ALTER TABLE IF EXISTS ONLY public.email_verifications DROP CONSTRAINT IF EXISTS email_verifications_pkey;
ALTER TABLE IF EXISTS ONLY public.email_verification_tokens DROP CONSTRAINT IF EXISTS email_verification_tokens_pkey;
ALTER TABLE IF EXISTS ONLY public.email_notifications DROP CONSTRAINT IF EXISTS email_notifications_tracking_token_key;
ALTER TABLE IF EXISTS ONLY public.email_notifications DROP CONSTRAINT IF EXISTS email_notifications_response_token_key;
ALTER TABLE IF EXISTS ONLY public.email_notifications DROP CONSTRAINT IF EXISTS email_notifications_pkey;
ALTER TABLE IF EXISTS ONLY public.email_events DROP CONSTRAINT IF EXISTS email_events_pkey;
ALTER TABLE IF EXISTS ONLY public.documents DROP CONSTRAINT IF EXISTS documents_pkey;
ALTER TABLE IF EXISTS ONLY public.applications DROP CONSTRAINT IF EXISTS applications_pkey;
ALTER TABLE IF EXISTS public.students ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.interviews ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.email_notifications ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.email_events ALTER COLUMN id DROP DEFAULT;
DROP TABLE IF EXISTS public.usuarios;
DROP TABLE IF EXISTS public.users;
DROP TABLE IF EXISTS public.temas;
DROP TABLE IF EXISTS public.supporters;
DROP TABLE IF EXISTS public.support_staff_responsibilities;
DROP TABLE IF EXISTS public.support_staff;
DROP SEQUENCE IF EXISTS public.students_id_seq;
DROP TABLE IF EXISTS public.students_backup;
DROP TABLE IF EXISTS public.students;
DROP TABLE IF EXISTS public.ranking;
DROP TABLE IF EXISTS public.psychologists;
DROP TABLE IF EXISTS public.psychologist_specialized_areas;
DROP TABLE IF EXISTS public.psychologist_grades;
DROP TABLE IF EXISTS public.progreso_usuario;
DROP TABLE IF EXISTS public.professors;
DROP TABLE IF EXISTS public.professor_subjects;
DROP TABLE IF EXISTS public.professor_qualifications;
DROP TABLE IF EXISTS public.professor_grades;
DROP TABLE IF EXISTS public.problemas;
DROP TABLE IF EXISTS public.parents;
DROP TABLE IF EXISTS public.kinder_teachers;
DROP TABLE IF EXISTS public.kinder_teacher_specializations;
DROP TABLE IF EXISTS public.kinder_teacher_qualifications;
DROP SEQUENCE IF EXISTS public.interviews_id_seq;
DROP TABLE IF EXISTS public.interviews;
DROP TABLE IF EXISTS public.guardians;
DROP TABLE IF EXISTS public.evaluations;
DROP TABLE IF EXISTS public.evaluation_schedules;
DROP TABLE IF EXISTS public.email_verifications;
DROP TABLE IF EXISTS public.email_verification_tokens;
DROP SEQUENCE IF EXISTS public.email_notifications_id_seq;
DROP TABLE IF EXISTS public.email_notifications;
DROP SEQUENCE IF EXISTS public.email_events_id_seq;
DROP TABLE IF EXISTS public.email_events;
DROP TABLE IF EXISTS public.documents;
DROP TABLE IF EXISTS public.applications;
DROP FUNCTION IF EXISTS public.validate_completed_interview();
DROP FUNCTION IF EXISTS public.update_interviews_updated_at();
DROP FUNCTION IF EXISTS public.update_email_notifications_updated_at();
DROP FUNCTION IF EXISTS public.generate_unique_token(length integer);
--
-- Name: generate_unique_token(integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.generate_unique_token(length integer DEFAULT 32) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    chars TEXT := 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    result TEXT := '';
    i INTEGER;
BEGIN
    FOR i IN 1..length LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::integer, 1);
    END LOOP;
    RETURN result;
END;
$$;


--
-- Name: update_email_notifications_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_email_notifications_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- Name: update_interviews_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_interviews_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- Name: validate_completed_interview(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.validate_completed_interview() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND NEW.result IS NULL THEN
        RAISE EXCEPTION 'Las entrevistas completadas deben tener un resultado';
    END IF;
    
    IF NEW.status = 'COMPLETED' AND NEW.completed_at IS NULL THEN
        NEW.completed_at = CURRENT_TIMESTAMP;
    END IF;
    
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: applications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.applications (
    id bigint NOT NULL,
    additional_notes text,
    created_at timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL,
    submission_date timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    applicant_user_id bigint,
    father_id bigint,
    guardian_id bigint,
    mother_id bigint,
    student_id bigint,
    supporter_id bigint,
    CONSTRAINT applications_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'UNDER_REVIEW'::character varying, 'DOCUMENTS_REQUESTED'::character varying, 'INTERVIEW_SCHEDULED'::character varying, 'EXAM_SCHEDULED'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'WAITLIST'::character varying])::text[])))
);


--
-- Name: applications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.applications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.applications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documents (
    id bigint NOT NULL,
    content_type character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    document_type character varying(255) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(255) NOT NULL,
    file_size bigint,
    is_required boolean NOT NULL,
    original_name character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    application_id bigint NOT NULL,
    CONSTRAINT documents_document_type_check CHECK (((document_type)::text = ANY ((ARRAY['BIRTH_CERTIFICATE'::character varying, 'GRADES_2023'::character varying, 'GRADES_2024'::character varying, 'GRADES_2025_SEMESTER_1'::character varying, 'PERSONALITY_REPORT_2024'::character varying, 'PERSONALITY_REPORT_2025_SEMESTER_1'::character varying, 'STUDENT_PHOTO'::character varying, 'BAPTISM_CERTIFICATE'::character varying, 'PREVIOUS_SCHOOL_REPORT'::character varying, 'MEDICAL_CERTIFICATE'::character varying, 'PSYCHOLOGICAL_REPORT'::character varying])::text[])))
);


--
-- Name: documents_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.documents ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.documents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: email_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_events (
    id bigint NOT NULL,
    email_notification_id bigint NOT NULL,
    event_type character varying(255) NOT NULL,
    ip_address character varying(45),
    user_agent text,
    additional_info jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: email_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.email_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: email_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.email_events_id_seq OWNED BY public.email_events.id;


--
-- Name: email_notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_notifications (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    recipient_email character varying(255) NOT NULL,
    email_type character varying(255) NOT NULL,
    subject character varying(500) NOT NULL,
    student_name character varying(255) NOT NULL,
    student_gender character varying(255) DEFAULT 'MALE'::character varying NOT NULL,
    target_school character varying(255) NOT NULL,
    sent_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    opened boolean DEFAULT false,
    opened_at timestamp without time zone,
    open_count integer DEFAULT 0,
    tracking_token character varying(255) NOT NULL,
    response_required boolean DEFAULT false,
    responded boolean DEFAULT false,
    response_value character varying(255),
    responded_at timestamp without time zone,
    response_token character varying(255),
    interview_id bigint,
    additional_data jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: email_notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.email_notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: email_notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.email_notifications_id_seq OWNED BY public.email_notifications.id;


--
-- Name: email_verification_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verification_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    is_expired boolean NOT NULL,
    is_used boolean NOT NULL,
    password_hash character varying(255) NOT NULL,
    token character varying(255) NOT NULL,
    username character varying(255) NOT NULL
);


--
-- Name: email_verification_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.email_verification_tokens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.email_verification_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: email_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verifications (
    id bigint NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    type character varying(255) NOT NULL,
    used boolean NOT NULL,
    used_at timestamp(6) without time zone,
    CONSTRAINT email_verifications_type_check CHECK (((type)::text = ANY ((ARRAY['REGISTRATION'::character varying, 'PASSWORD_RESET'::character varying])::text[])))
);


--
-- Name: email_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.email_verifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.email_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: evaluation_schedules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evaluation_schedules (
    id bigint NOT NULL,
    attendees_required text,
    confirmation_deadline timestamp(6) without time zone,
    confirmed_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    duration_minutes integer NOT NULL,
    evaluation_type character varying(255),
    grade_level character varying(255),
    instructions text,
    location character varying(255),
    meeting_link character varying(255),
    preparation_materials text,
    requires_confirmation boolean,
    schedule_type character varying(255) NOT NULL,
    scheduled_date timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL,
    subject character varying(255),
    updated_at timestamp(6) without time zone,
    application_id bigint,
    confirmed_by_user_id bigint,
    evaluator_id bigint,
    CONSTRAINT evaluation_schedules_evaluation_type_check CHECK (((evaluation_type)::text = ANY ((ARRAY['LANGUAGE_EXAM'::character varying, 'MATHEMATICS_EXAM'::character varying, 'ENGLISH_EXAM'::character varying, 'CYCLE_DIRECTOR_REPORT'::character varying, 'CYCLE_DIRECTOR_INTERVIEW'::character varying, 'PSYCHOLOGICAL_INTERVIEW'::character varying])::text[]))),
    CONSTRAINT evaluation_schedules_schedule_type_check CHECK (((schedule_type)::text = ANY ((ARRAY['GENERIC'::character varying, 'INDIVIDUAL'::character varying, 'GROUP'::character varying, 'MAKEUP'::character varying])::text[]))),
    CONSTRAINT evaluation_schedules_status_check CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'CONFIRMED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'RESCHEDULED'::character varying, 'NO_SHOW'::character varying])::text[])))
);


--
-- Name: evaluation_schedules_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.evaluation_schedules ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.evaluation_schedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: evaluations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evaluations (
    id bigint NOT NULL,
    academic_readiness text,
    areas_for_improvement text,
    behavioral_assessment text,
    completion_date timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    emotional_maturity text,
    evaluation_date timestamp(6) without time zone,
    evaluation_type character varying(255) NOT NULL,
    family_support_assessment text,
    final_recommendation boolean,
    grade character varying(255),
    integration_potential text,
    motivation_assessment text,
    observations text,
    recommendations text,
    score integer,
    social_skills_assessment text,
    status character varying(255) NOT NULL,
    strengths text,
    updated_at timestamp(6) without time zone,
    application_id bigint NOT NULL,
    evaluator_id bigint NOT NULL,
    schedule_id bigint,
    CONSTRAINT evaluations_evaluation_type_check CHECK (((evaluation_type)::text = ANY ((ARRAY['LANGUAGE_EXAM'::character varying, 'MATHEMATICS_EXAM'::character varying, 'ENGLISH_EXAM'::character varying, 'CYCLE_DIRECTOR_REPORT'::character varying, 'CYCLE_DIRECTOR_INTERVIEW'::character varying, 'PSYCHOLOGICAL_INTERVIEW'::character varying])::text[]))),
    CONSTRAINT evaluations_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'REVIEWED'::character varying, 'APPROVED'::character varying])::text[])))
);


--
-- Name: evaluations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.evaluations ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.evaluations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: guardians; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.guardians (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    phone character varying(255) NOT NULL,
    relationship character varying(255) NOT NULL,
    rut character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT guardians_relationship_check CHECK (((relationship)::text = ANY ((ARRAY['PADRE'::character varying, 'MADRE'::character varying, 'ABUELO'::character varying, 'TIO'::character varying, 'HERMANO'::character varying, 'TUTOR'::character varying, 'OTRO'::character varying])::text[])))
);


--
-- Name: guardians_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.guardians ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.guardians_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: interviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.interviews (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    interviewer_user_id bigint NOT NULL,
    status character varying(255) DEFAULT 'SCHEDULED'::character varying NOT NULL,
    type character varying(255) NOT NULL,
    mode character varying(255) NOT NULL,
    scheduled_date date NOT NULL,
    scheduled_time time without time zone NOT NULL,
    duration integer NOT NULL,
    location character varying(500),
    virtual_meeting_link character varying(1000),
    notes text,
    preparation text,
    result character varying(255),
    score real,
    recommendations text,
    follow_up_required boolean DEFAULT false NOT NULL,
    follow_up_notes text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    completed_at timestamp with time zone,
    CONSTRAINT interviews_duration_check CHECK (((duration >= 15) AND (duration <= 480))),
    CONSTRAINT interviews_mode_check CHECK (((mode)::text = ANY (ARRAY[('IN_PERSON'::character varying)::text, ('VIRTUAL'::character varying)::text, ('HYBRID'::character varying)::text]))),
    CONSTRAINT interviews_result_check CHECK (((result)::text = ANY (ARRAY[('POSITIVE'::character varying)::text, ('NEUTRAL'::character varying)::text, ('NEGATIVE'::character varying)::text, ('PENDING_REVIEW'::character varying)::text, ('REQUIRES_FOLLOW_UP'::character varying)::text]))),
    CONSTRAINT interviews_score_check CHECK (((score >= (1.0)::double precision) AND (score <= (10.0)::double precision))),
    CONSTRAINT interviews_status_check CHECK (((status)::text = ANY (ARRAY[('SCHEDULED'::character varying)::text, ('CONFIRMED'::character varying)::text, ('IN_PROGRESS'::character varying)::text, ('COMPLETED'::character varying)::text, ('CANCELLED'::character varying)::text, ('NO_SHOW'::character varying)::text, ('RESCHEDULED'::character varying)::text]))),
    CONSTRAINT interviews_type_check CHECK (((type)::text = ANY (ARRAY[('INDIVIDUAL'::character varying)::text, ('FAMILY'::character varying)::text, ('PSYCHOLOGICAL'::character varying)::text, ('ACADEMIC'::character varying)::text, ('BEHAVIORAL'::character varying)::text])))
);


--
-- Name: TABLE interviews; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.interviews IS 'Tabla para gestionar las entrevistas de admisión';


--
-- Name: COLUMN interviews.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.interviews.status IS 'Estado actual de la entrevista: SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED';


--
-- Name: COLUMN interviews.type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.interviews.type IS 'Tipo de entrevista: INDIVIDUAL, FAMILY, PSYCHOLOGICAL, ACADEMIC, BEHAVIORAL';


--
-- Name: COLUMN interviews.mode; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.interviews.mode IS 'Modalidad: IN_PERSON, VIRTUAL, HYBRID';


--
-- Name: COLUMN interviews.duration; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.interviews.duration IS 'Duración en minutos (15-480)';


--
-- Name: COLUMN interviews.score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.interviews.score IS 'Puntuación de 1.0 a 10.0';


--
-- Name: COLUMN interviews.follow_up_required; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.interviews.follow_up_required IS 'Indica si la entrevista requiere seguimiento';


--
-- Name: interviews_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.interviews_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: interviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.interviews_id_seq OWNED BY public.interviews.id;


--
-- Name: kinder_teacher_qualifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kinder_teacher_qualifications (
    teacher_id bigint NOT NULL,
    qualification character varying(255)
);


--
-- Name: kinder_teacher_specializations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kinder_teacher_specializations (
    teacher_id bigint NOT NULL,
    specialization character varying(255)
);


--
-- Name: kinder_teachers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kinder_teachers (
    id bigint NOT NULL,
    assigned_level character varying(255),
    years_of_experience integer,
    CONSTRAINT kinder_teachers_assigned_level_check CHECK (((assigned_level)::text = ANY ((ARRAY['PREKINDER'::character varying, 'KINDER'::character varying])::text[])))
);


--
-- Name: parents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parents (
    id bigint NOT NULL,
    address character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    parent_type character varying(255) NOT NULL,
    phone character varying(255) NOT NULL,
    profession character varying(255) NOT NULL,
    rut character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT parents_parent_type_check CHECK (((parent_type)::text = ANY ((ARRAY['FATHER'::character varying, 'MOTHER'::character varying])::text[])))
);


--
-- Name: parents_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.parents ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.parents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: problemas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.problemas (
    id bigint NOT NULL,
    codigo_inicial text,
    descripcion text,
    dificultad character varying(255) NOT NULL,
    solucion_correcta text,
    test_cases_json text,
    titulo character varying(255) NOT NULL,
    tema_id bigint,
    CONSTRAINT problemas_dificultad_check CHECK (((dificultad)::text = ANY ((ARRAY['EASY'::character varying, 'INTERMEDIATE'::character varying, 'HARD'::character varying])::text[])))
);


--
-- Name: professor_grades; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.professor_grades (
    professor_id bigint NOT NULL,
    grade character varying(255)
);


--
-- Name: professor_qualifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.professor_qualifications (
    professor_id bigint NOT NULL,
    qualification character varying(255)
);


--
-- Name: professor_subjects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.professor_subjects (
    professor_id bigint NOT NULL,
    subject character varying(255),
    CONSTRAINT professor_subjects_subject_check CHECK (((subject)::text = ANY ((ARRAY['MATH'::character varying, 'SPANISH'::character varying, 'ENGLISH'::character varying])::text[])))
);


--
-- Name: professors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.professors (
    id bigint NOT NULL,
    department character varying(255),
    is_admin boolean NOT NULL,
    years_of_experience integer
);


--
-- Name: progreso_usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progreso_usuario (
    id bigint NOT NULL,
    estado character varying(255),
    intentos integer NOT NULL,
    ultima_modificacion timestamp(6) without time zone,
    problema_id bigint,
    usuario_id bigint,
    CONSTRAINT progreso_usuario_estado_check CHECK (((estado)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'IN_PROGRESS'::character varying, 'SOLVED'::character varying])::text[])))
);


--
-- Name: progreso_usuario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.progreso_usuario ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.progreso_usuario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: psychologist_grades; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.psychologist_grades (
    psychologist_id bigint NOT NULL,
    grade character varying(255)
);


--
-- Name: psychologist_specialized_areas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.psychologist_specialized_areas (
    psychologist_id bigint NOT NULL,
    area character varying(255)
);


--
-- Name: psychologists; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.psychologists (
    id bigint NOT NULL,
    can_conduct_interviews boolean NOT NULL,
    can_perform_psychological_evaluations boolean NOT NULL,
    license_number character varying(255),
    specialty character varying(255),
    CONSTRAINT psychologists_specialty_check CHECK (((specialty)::text = ANY ((ARRAY['EDUCATIONAL'::character varying, 'CLINICAL'::character varying, 'DEVELOPMENTAL'::character varying, 'COGNITIVE'::character varying])::text[])))
);


--
-- Name: ranking; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ranking (
    id bigint NOT NULL,
    fecha_actualizacion timestamp(6) without time zone,
    puntaje integer NOT NULL,
    usuario_id bigint
);


--
-- Name: ranking_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.ranking ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.ranking_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: students; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.students (
    id bigint NOT NULL,
    additional_notes text,
    address character varying(255) NOT NULL,
    birth_date date NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    current_school character varying(255),
    email character varying(255),
    first_name character varying(255) NOT NULL,
    grade_applied character varying(255) NOT NULL,
    maternal_last_name character varying(255) NOT NULL,
    paternal_last_name character varying(255) NOT NULL,
    rut character varying(255) NOT NULL,
    school_applied character varying(255),
    updated_at timestamp(6) without time zone,
    is_employee_child boolean DEFAULT false,
    is_alumni_child boolean DEFAULT false,
    is_inclusion_student boolean DEFAULT false,
    employee_parent_name character varying(255),
    alumni_parent_year integer,
    inclusion_type character varying(100),
    inclusion_notes text,
    age integer,
    target_school character varying(50) DEFAULT 'MONTE_TABOR'::character varying
);


--
-- Name: students_backup; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.students_backup (
    id bigint,
    additional_notes text,
    address character varying(255),
    birth_date date,
    created_at timestamp(6) without time zone,
    current_school character varying(255),
    email character varying(255),
    first_name character varying(255),
    grade_applied character varying(255),
    rut character varying(255),
    updated_at timestamp(6) without time zone,
    paternal_last_name character varying(255),
    maternal_last_name character varying(255),
    school_applied character varying(255)
);


--
-- Name: students_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.students_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: students_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.students_id_seq OWNED BY public.students.id;


--
-- Name: support_staff; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.support_staff (
    id bigint NOT NULL,
    can_access_reports boolean NOT NULL,
    can_manage_schedules boolean NOT NULL,
    department character varying(255),
    staff_type character varying(255),
    CONSTRAINT support_staff_staff_type_check CHECK (((staff_type)::text = ANY ((ARRAY['ADMINISTRATIVE'::character varying, 'TECHNICAL'::character varying, 'ACADEMIC_COORDINATOR'::character varying, 'STUDENT_SERVICES'::character varying, 'IT_SUPPORT'::character varying])::text[])))
);


--
-- Name: support_staff_responsibilities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.support_staff_responsibilities (
    staff_id bigint NOT NULL,
    responsibility character varying(255)
);


--
-- Name: supporters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supporters (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    phone character varying(255) NOT NULL,
    relationship character varying(255) NOT NULL,
    rut character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT supporters_relationship_check CHECK (((relationship)::text = ANY ((ARRAY['PADRE'::character varying, 'MADRE'::character varying, 'ABUELO'::character varying, 'TIO'::character varying, 'HERMANO'::character varying, 'TUTOR'::character varying, 'OTRO'::character varying])::text[])))
);


--
-- Name: supporters_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.supporters ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.supporters_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: temas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.temas (
    id bigint NOT NULL,
    descripcion character varying(255),
    dificultad character varying(255),
    nombre character varying(255) NOT NULL,
    CONSTRAINT temas_dificultad_check CHECK (((dificultad)::text = ANY ((ARRAY['EASY'::character varying, 'INTERMEDIATE'::character varying, 'HARD'::character varying])::text[])))
);


--
-- Name: temas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.temas ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.temas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    active boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    email_verified boolean NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    phone character varying(255),
    role character varying(255) NOT NULL,
    rut character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    educational_level character varying(255),
    subject character varying(255),
    CONSTRAINT users_educational_level_check CHECK (((educational_level IS NULL) OR ((educational_level)::text = ANY ((ARRAY['PRESCHOOL'::character varying, 'BASIC'::character varying, 'HIGH_SCHOOL'::character varying, 'ALL_LEVELS'::character varying])::text[])))),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['APODERADO'::character varying, 'ADMIN'::character varying, 'TEACHER'::character varying, 'COORDINATOR'::character varying, 'PSYCHOLOGIST'::character varying, 'CYCLE_DIRECTOR'::character varying])::text[]))),
    CONSTRAINT users_subject_check CHECK (((subject IS NULL) OR ((subject)::text = ANY ((ARRAY['GENERAL'::character varying, 'LANGUAGE'::character varying, 'MATHEMATICS'::character varying, 'ENGLISH'::character varying, 'ALL_SUBJECTS'::character varying])::text[]))))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.users ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id bigint NOT NULL,
    email character varying(255) NOT NULL,
    email_verified boolean NOT NULL,
    fecha_registro timestamp(6) without time zone,
    first_name character varying(255),
    is_active boolean NOT NULL,
    last_name character varying(255),
    password character varying(60) NOT NULL,
    phone character varying(255),
    profile_image character varying(255),
    puntaje integer NOT NULL,
    rol character varying(255),
    updated_at timestamp(6) without time zone,
    username character varying(255) NOT NULL,
    CONSTRAINT usuarios_rol_check CHECK (((rol)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying, 'PROFESSOR'::character varying, 'KINDER_TEACHER'::character varying, 'PSYCHOLOGIST'::character varying, 'SUPPORT_STAFF'::character varying])::text[])))
);


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: email_events id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_events ALTER COLUMN id SET DEFAULT nextval('public.email_events_id_seq'::regclass);


--
-- Name: email_notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_notifications ALTER COLUMN id SET DEFAULT nextval('public.email_notifications_id_seq'::regclass);


--
-- Name: interviews id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interviews ALTER COLUMN id SET DEFAULT nextval('public.interviews_id_seq'::regclass);


--
-- Name: students id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students ALTER COLUMN id SET DEFAULT nextval('public.students_id_seq'::regclass);


--
-- Data for Name: applications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.applications (id, additional_notes, created_at, status, submission_date, updated_at, applicant_user_id, father_id, guardian_id, mother_id, student_id, supporter_id) FROM stdin;
9	POSTULACIÓN COMPLETA DE PRUEBA	2025-08-22 04:00:20.718414	PENDING	2025-08-22 04:00:20.718414	2025-08-22 04:00:20.718414	17	17	9	18	1	9
10	Primera postulación para prekinder	2025-01-15 10:30:00	PENDING	2025-01-15 10:30:00	2025-01-15 10:30:00	72	\N	\N	\N	2	\N
11	Estudiante viene de jardín privado	2025-01-12 14:20:00	UNDER_REVIEW	2025-01-12 14:20:00	2025-01-18 09:15:00	73	\N	\N	\N	3	\N
12	Entrevista programada para febrero	2025-01-10 09:15:00	INTERVIEW_SCHEDULED	2025-01-10 09:15:00	2025-01-20 11:30:00	74	\N	\N	\N	4	\N
13	Estudiante con necesidades especiales (TDAH)	2025-01-08 16:45:00	EXAM_SCHEDULED	2025-01-08 16:45:00	2025-01-22 14:20:00	75	\N	\N	\N	5	\N
14	Postulación aprobada con excelentes calificaciones	2025-01-05 11:00:00	APPROVED	2025-01-05 11:00:00	2025-01-25 10:00:00	76	\N	\N	\N	6	\N
15	Documentos pendientes de entrega	2025-01-20 08:30:00	PENDING	2025-01-20 08:30:00	2025-01-20 08:30:00	77	\N	\N	\N	7	\N
16	Estudiante con dislexia leve, en evaluación	2025-01-18 13:15:00	UNDER_REVIEW	2025-01-18 13:15:00	2025-01-22 16:00:00	78	\N	\N	\N	8	\N
17	En lista de espera por cupos limitados	2025-01-14 15:20:00	WAITLIST	2025-01-14 15:20:00	2025-01-28 12:45:00	79	\N	\N	\N	9	\N
18	No cumple con requisitos académicos mínimos	2025-01-06 12:00:00	REJECTED	2025-01-06 12:00:00	2025-01-30 09:30:00	80	\N	\N	\N	10	\N
19	Estudiante con discapacidad auditiva leve	2025-01-22 10:45:00	INTERVIEW_SCHEDULED	2025-01-22 10:45:00	2025-02-01 14:15:00	81	\N	\N	\N	11	\N
21	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	84	59	12	60	32	12
22	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	85	61	13	62	34	13
23	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	86	63	14	64	36	14
24	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	87	65	15	66	38	15
25	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	88	67	16	68	40	16
26	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	89	69	17	70	42	17
27	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	90	71	18	72	44	18
28	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	91	73	19	74	46	19
29	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	92	75	20	76	48	20
30	\N	2025-08-23 02:45:09.639885	PENDING	2025-08-18 02:45:09.639885	\N	93	77	21	78	50	21
\.


--
-- Data for Name: documents; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.documents (id, content_type, created_at, document_type, file_name, file_path, file_size, is_required, original_name, updated_at, application_id) FROM stdin;
\.


--
-- Data for Name: email_events; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.email_events (id, email_notification_id, event_type, ip_address, user_agent, additional_info, created_at) FROM stdin;
1	5	OPENED	0:0:0:0:0:0:0:1	curl/8.7.1	{}	2025-08-23 17:10:56.912777
2	5	RESPONDED	0:0:0:0:0:0:0:1	curl/8.7.1	{"response": "REJECT"}	2025-08-23 17:11:01.605117
\.


--
-- Data for Name: email_notifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.email_notifications (id, application_id, recipient_email, email_type, subject, student_name, student_gender, target_school, sent_at, opened, opened_at, open_count, tracking_token, response_required, responded, response_value, responded_at, response_token, interview_id, additional_data, created_at, updated_at) FROM stdin;
6	11	familia03@test.cl	INTERVIEW_REMINDER	Recordatorio de Entrevista - María Elena (Colegio Nazaret)	María Elena	FEMALE	NAZARET	2025-08-23 14:34:21.264264	f	\N	0	track_ghi789	f	f	\N	\N	\N	\N	{}	2025-08-23 14:34:21.264264	2025-08-23 14:34:21.264264
4	9	familia01@test.cl	INTERVIEW_SCHEDULED	Entrevista Programada - Ana María (Colegio Monte Tabor)	Ana María	FEMALE	MONTE_TABOR	2025-08-23 14:34:21.264264	t	2025-08-23 17:09:22.101036	1	track_abc123	t	t	ACCEPT	2025-08-23 17:09:40.538449	resp_xyz789	\N	{}	2025-08-23 14:34:21.264264	2025-08-23 13:09:40.539263
5	10	familia02@test.cl	INTERVIEW_SCHEDULED	Entrevista Programada - Juan Carlos (Colegio Monte Tabor)	Juan Carlos	MALE	MONTE_TABOR	2025-08-23 14:34:21.264264	t	2025-08-23 17:10:56.908442	1	track_def456	t	t	REJECT	2025-08-23 17:11:01.601904	resp_uvw456	\N	{}	2025-08-23 14:34:21.264264	2025-08-23 13:11:01.6022
\.


--
-- Data for Name: email_verification_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.email_verification_tokens (id, created_at, email, expires_at, is_expired, is_used, password_hash, token, username) FROM stdin;
\.


--
-- Data for Name: email_verifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.email_verifications (id, code, created_at, email, expires_at, type, used, used_at) FROM stdin;
4	547076	2025-08-17 17:45:31.021409	jorge.gangale@gmail.com	2025-08-17 17:55:31.021401	REGISTRATION	t	2025-08-17 17:45:55.014078
6	635372	2025-08-18 15:29:56.963867	jorge.gangale@mail.udp.cl	2025-08-18 15:39:56.963862	REGISTRATION	t	2025-08-18 15:30:24.926254
7	265625	2025-08-18 19:37:27.17733	schweikart.cr@gmail.com	2025-08-18 19:47:27.177316	REGISTRATION	t	2025-08-18 19:38:02.849821
\.


--
-- Data for Name: evaluation_schedules; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.evaluation_schedules (id, attendees_required, confirmation_deadline, confirmed_at, created_at, duration_minutes, evaluation_type, grade_level, instructions, location, meeting_link, preparation_materials, requires_confirmation, schedule_type, scheduled_date, status, subject, updated_at, application_id, confirmed_by_user_id, evaluator_id) FROM stdin;
\.


--
-- Data for Name: evaluations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.evaluations (id, academic_readiness, areas_for_improvement, behavioral_assessment, completion_date, created_at, emotional_maturity, evaluation_date, evaluation_type, family_support_assessment, final_recommendation, grade, integration_potential, motivation_assessment, observations, recommendations, score, social_skills_assessment, status, strengths, updated_at, application_id, evaluator_id, schedule_id) FROM stdin;
29	Buen dominio de conceptos matemáticos básicos para su nivel	Mejorar velocidad de cálculo mental	Concentrado durante la evaluación, sigue instrucciones	2025-08-21 03:22:24.235454	2025-08-16 03:22:24.235454	Manejo adecuado de situaciones de evaluación	2025-08-16 03:22:24.235454	MATHEMATICS_EXAM	\N	t	3° BÁSICO	\N	Muestra interés por actividades matemáticas	Evaluación completada satisfactoriamente	Estudiante apto para el nivel solicitado	94	Interactúa bien con el evaluador	COMPLETED	Razonamiento lógico, resolución de problemas	2025-08-21 03:22:24.235454	9	55	\N
30	Buen dominio de conceptos matemáticos básicos para su nivel	Mejorar velocidad de cálculo mental	Concentrado durante la evaluación, sigue instrucciones	2025-08-21 03:22:24.235454	2025-08-16 03:22:24.235454	Manejo adecuado de situaciones de evaluación	2025-08-16 03:22:24.235454	MATHEMATICS_EXAM	\N	t	Prekinder	\N	Muestra interés por actividades matemáticas	Evaluación completada satisfactoriamente	Estudiante apto para el nivel solicitado	95	Interactúa bien con el evaluador	COMPLETED	Razonamiento lógico, resolución de problemas	2025-08-21 03:22:24.235454	10	55	\N
31	Desarrollo apropiado del lenguaje oral y comprensión	Ampliar vocabulario técnico	Participativo y expresivo durante actividades	2025-08-22 03:22:24.240388	2025-08-18 03:22:24.240388	Confianza al comunicarse verbalmente	2025-08-18 03:22:24.240388	LANGUAGE_EXAM	\N	t	3° BÁSICO	\N	Disfruta actividades de lectura inicial	Evaluación de lenguaje en desarrollo	Nivel adecuado para ingreso	89	Comunicativo y respetuoso	COMPLETED	Comprensión lectora, expresión verbal	2025-08-22 03:22:24.240388	9	56	\N
32	\N	\N	\N	\N	2025-08-18 03:22:24.240388	\N	2025-08-18 03:22:24.240388	LANGUAGE_EXAM	\N	\N	Prekinder	\N	\N	Evaluación de lenguaje en desarrollo	\N	\N	\N	IN_PROGRESS	\N	2025-08-22 03:22:24.240388	10	56	\N
33	\N	\N	\N	\N	2025-08-18 03:22:24.240388	\N	2025-08-18 03:22:24.240388	LANGUAGE_EXAM	\N	\N	Kinder	\N	\N	Evaluación de lenguaje en desarrollo	\N	\N	\N	PENDING	\N	2025-08-22 03:22:24.240388	11	56	\N
34	Desarrollo apropiado del lenguaje oral y comprensión	Ampliar vocabulario técnico	Participativo y expresivo durante actividades	2025-08-22 03:22:24.240388	2025-08-18 03:22:24.240388	Confianza al comunicarse verbalmente	2025-08-18 03:22:24.240388	LANGUAGE_EXAM	\N	t	1° Básico	\N	Disfruta actividades de lectura inicial	Evaluación de lenguaje en desarrollo	Nivel adecuado para ingreso	92	Comunicativo y respetuoso	COMPLETED	Comprensión lectora, expresión verbal	2025-08-22 03:22:24.240388	12	56	\N
\.


--
-- Data for Name: guardians; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.guardians (id, created_at, email, full_name, phone, relationship, rut, updated_at) FROM stdin;
3	2025-08-17 17:48:51.648787	alejandra.flores@mail.uc.cl	alejandra flores	+56983620169	MADRE	12.265.495-8	2025-08-17 17:48:51.648794
4	2025-08-17 21:05:56.199704	juanaperez@123.cl	juana perez	+569876543	MADRE	12.265.495-8	2025-08-17 21:05:56.199742
5	2025-08-17 22:21:23.828287	maria.gonzalez@email.com	María González	+56987654321	MADRE	22222222-2	2025-08-17 22:21:23.828293
6	2025-08-17 22:43:44.162705	juanaperez@123.cl	juana perez	+569876543	MADRE	12.847.031-K	2025-08-17 22:43:44.162723
7	2025-08-17 22:52:19.348467	ana.lopez@email.com	Ana Lopez	+56922334455	MADRE	16666666-6	2025-08-17 22:52:19.348474
8	2025-08-18 19:43:40.354021	juanaperez@123.cl	juana perez	+569876543	MADRE	12.265.495-8	2025-08-18 19:43:40.35403
9	2025-08-22 04:00:20.717859	maria.perez@mail.com	MARÍA PÉREZ SÁNCHEZ	+56976543210	MADRE	76543210-1	2025-08-22 04:00:20.717859
12	2025-08-23 02:44:47.136685	familia01@test.cl	María José González Silva	+56911000001	MADRE	40003001-3	\N
13	2025-08-23 02:44:47.136685	familia02@test.cl	Carmen Rodríguez López	+56911000002	MADRE	40003002-1	\N
14	2025-08-23 02:44:47.136685	familia03@test.cl	Patricia Morales Castro	+56911000003	MADRE	40003003-K	\N
15	2025-08-23 02:44:47.136685	familia04@test.cl	Andrea Silva Torres	+56911000004	MADRE	40003004-8	\N
16	2025-08-23 02:44:47.136685	familia05@test.cl	Claudia Hernández Pérez	+56911000005	MADRE	40003005-6	\N
17	2025-08-23 02:44:47.136685	familia06@test.cl	Valeria Castro Morales	+56911000006	MADRE	40003006-4	\N
18	2025-08-23 02:44:47.136685	familia07@test.cl	Mónica Vargas Silva	+56911000007	MADRE	40003007-2	\N
19	2025-08-23 02:44:47.136685	familia08@test.cl	Francisca Muñoz González	+56911000008	MADRE	40003008-0	\N
20	2025-08-23 02:44:47.136685	familia09@test.cl	Soledad Torres Hernández	+56911000009	MADRE	40003009-9	\N
21	2025-08-23 02:44:47.136685	familia10@test.cl	Alejandra Pérez Castro	+56911000010	MADRE	40003010-3	\N
22	2025-08-23 02:44:47.136685	familia11@test.cl	Lorena López Vargas	+56911000011	MADRE	40003011-1	\N
23	2025-08-23 02:44:47.136685	familia12@test.cl	Paola García Muñoz	+56911000012	MADRE	40003012-K	\N
24	2025-08-23 02:44:47.136685	familia13@test.cl	Verónica Martínez Torres	+56911000013	MADRE	40003013-8	\N
25	2025-08-23 02:44:47.136685	familia14@test.cl	Carolina Sánchez Pérez	+56911000014	MADRE	40003014-6	\N
26	2025-08-23 02:44:47.136685	familia15@test.cl	Daniela Ramos López	+56911000015	MADRE	40003015-4	\N
27	2025-08-23 02:44:47.136685	familia16@test.cl	Marcela Flores García	+56911000016	MADRE	40003016-2	\N
28	2025-08-23 02:44:47.136685	familia17@test.cl	Gladys Contreras Martínez	+56911000017	MADRE	40003017-0	\N
29	2025-08-23 02:44:47.136685	familia18@test.cl	Cecilia Parra Sánchez	+56911000018	MADRE	40003018-9	\N
30	2025-08-23 02:44:47.136685	familia19@test.cl	Roxana Aguilar Ramos	+56911000019	MADRE	40003019-7	\N
31	2025-08-23 02:44:47.136685	familia20@test.cl	Ingrid Fuentes Flores	+56911000020	MADRE	40003020-1	\N
\.


--
-- Data for Name: interviews; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.interviews (id, application_id, interviewer_user_id, status, type, mode, scheduled_date, scheduled_time, duration, location, virtual_meeting_link, notes, preparation, result, score, recommendations, follow_up_required, follow_up_notes, created_at, updated_at, completed_at) FROM stdin;
7	9	32	SCHEDULED	FAMILY	IN_PERSON	2025-08-30	10:00:00	45	Sala de Reuniones - Monte Tabor	\N	Entrevista familiar para proceso de admisión 2025	\N	\N	\N	\N	f	\N	2025-08-23 14:41:48.801741+00	2025-08-23 14:41:48.801741+00	\N
\.


--
-- Data for Name: kinder_teacher_qualifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.kinder_teacher_qualifications (teacher_id, qualification) FROM stdin;
\.


--
-- Data for Name: kinder_teacher_specializations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.kinder_teacher_specializations (teacher_id, specialization) FROM stdin;
\.


--
-- Data for Name: kinder_teachers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.kinder_teachers (id, assigned_level, years_of_experience) FROM stdin;
\.


--
-- Data for Name: parents; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.parents (id, address, created_at, email, full_name, parent_type, phone, profession, rut, updated_at) FROM stdin;
5	palqui 2916	2025-08-17 17:48:51.646607	jorge.gangale@gmail.com	jorge gangale	FATHER	+56983620169	profesor de matematica	11.650.825-7	2025-08-17 17:48:51.646623
6	palqui 2916	2025-08-17 17:48:51.650363	alejandra.flores@mail.uc.cl	alejandra flores	MOTHER	+56983620169	rofesora de matematica	12.265.495-8	2025-08-17 17:48:51.65037
7	palqui 2916 depto 62	2025-08-17 21:05:56.191312	jorge.gangale@gmail.com	jorge gangale	FATHER	+56983621068	Ingeniero comercial	11.650.825-7	2025-08-17 21:05:56.191351
8	palqui 2916 depto 62	2025-08-17 21:05:56.20364	juanaperez@123.cl	juana perez	MOTHER	+569876543	Doctor	12.265.495-8	2025-08-17 21:05:56.203655
9	Av. Providencia 123, Santiago	2025-08-17 22:21:23.826338	carlos.perez@email.com	Carlos Pérez	FATHER	+56912345678	Ingeniero	11111111-1	2025-08-17 22:21:23.826353
10	Av. Providencia 123, Santiago	2025-08-17 22:21:23.829309	maria.gonzalez@email.com	María González	MOTHER	+56987654321	Profesora	22222222-2	2025-08-17 22:21:23.829319
11	palqui 2916 depto 62	2025-08-17 22:43:44.159601	jorge.gangale@gmail.com	jorge gangale	FATHER	+56983621068	Ingeniero comercial	11.650.825-7	2025-08-17 22:43:44.159617
12	palqui 2916 depto 62	2025-08-17 22:43:44.164997	juanaperez@123.cl	juana perez	MOTHER	+569876543	Doctor	12.847.031-K	2025-08-17 22:43:44.165007
13	Av. Las Condes 789, Santiago	2025-08-17 22:52:19.340782	roberto.martinez@email.com	Roberto Martinez	FATHER	+56911223344	Arquitecto	15555555-5	2025-08-17 22:52:19.340796
14	Av. Las Condes 789, Santiago	2025-08-17 22:52:19.350089	ana.lopez@email.com	Ana Lopez	MOTHER	+56922334455	Diseñadora	16666666-6	2025-08-17 22:52:19.350094
15	palqui 2916 depto 62	2025-08-18 19:43:40.351639	schweikart.cr@gmail.com	cristian schweikart	FATHER	+56987654321	Ingeniero comercial	18.879.517-K	2025-08-18 19:43:40.351654
16	palqui 2916 depto 62	2025-08-18 19:43:40.355397	juanaperez@123.cl	juana perez	MOTHER	+569876543	Doctor	12.265.495-8	2025-08-18 19:43:40.355401
17	AV. PROVIDENCIA 1234, SANTIAGO	2025-08-22 04:00:20.713721	carlos.gonzalez@mail.com	CARLOS GONZÁLEZ MARTÍNEZ	FATHER	+56987654321	INGENIERO CIVIL	87654321-0	2025-08-22 04:00:20.713721
18	AV. PROVIDENCIA 1234, SANTIAGO	2025-08-22 04:00:20.715996	maria.perez@mail.com	MARÍA PÉREZ SÁNCHEZ	MOTHER	+56976543210	PROFESORA DE EDUCACIÓN BÁSICA	76543210-1	2025-08-22 04:00:20.715996
19	Las Condes 1234, Las Condes	2025-08-22 21:52:05.565246	carlos.gonzalez@gmail.com	Carlos González Hernández	FATHER	+56987111111	Ingeniero Civil	11111111-1	\N
21	Providencia 567, Providencia	2025-08-22 21:52:05.565246	pedro.rodriguez@gmail.com	Pedro Rodríguez Morales	FATHER	+56987111112	Contador	11111113-8	\N
23	Ñuñoa 890, Ñuñoa	2025-08-22 21:52:05.565246	luis.morales@gmail.com	Luis Morales Pérez	FATHER	+56987111113	Médico	11111115-4	\N
25	La Reina 2345, La Reina	2025-08-22 21:52:05.565246	roberto.silva@gmail.com	Roberto Silva Vargas	FATHER	+56987111114	Arquitecto	11111117-0	\N
27	Vitacura 3456, Vitacura	2025-08-22 21:52:05.565246	miguel.hernandez@gmail.com	Miguel Hernández Silva	FATHER	+56987111115	Abogado	11111119-7	\N
29	Peñalolén 4567, Peñalolén	2025-08-22 21:52:05.565246	jorge.castro@gmail.com	Jorge Castro Torres	FATHER	+56987111116	Veterinario	11111121-9	\N
31	La Florida 5678, La Florida	2025-08-22 21:52:05.565246	andres.vargas@gmail.com	Andrés Vargas González	FATHER	+56987111117	Periodista	11111123-5	\N
33	Maipú 6789, Maipú	2025-08-22 21:52:05.565246	felipe.munoz@gmail.com	Felipe Muñoz Hernández	FATHER	+56987111118	Dentista	11111125-1	\N
35	San Miguel 7890, San Miguel	2025-08-22 21:52:05.565246	patricio.torres@gmail.com	Patricio Torres Castro	FATHER	+56987111119	Electricista	11111127-8	\N
37	Quilicura 8901, Quilicura	2025-08-22 21:52:05.565246	rodrigo.perez@gmail.com	Rodrigo Pérez Vargas	FATHER	+56987111120	Mecánico	11111129-4	\N
39	Puente Alto 9012, Puente Alto	2025-08-22 21:52:05.565246	cristian.lopez@gmail.com	Cristián López Muñoz	FATHER	+56987111121	Profesor	11111131-6	\N
41	Renca 0123, Renca	2025-08-22 21:52:05.565246	gonzalo.garcia@gmail.com	Gonzalo García Torres	FATHER	+56987111122	Soldador	11111133-2	\N
43	Cerro Navia 1234, Cerro Navia	2025-08-22 21:52:05.565246	fernando.martinez@gmail.com	Fernando Martínez González	FATHER	+56987111123	Carpintero	11111135-9	\N
45	Estación Central 2345, Estación Central	2025-08-22 21:52:05.565246	mauricio.sanchez@gmail.com	Mauricio Sánchez Hernández	FATHER	+56987111124	Guardia	11111137-5	\N
47	Pedro Aguirre Cerda 3456, Pedro Aguirre Cerda	2025-08-22 21:52:05.565246	osvaldo.ramos@gmail.com	Osvaldo Ramos Castro	FATHER	+56987111125	Conductor	11111139-1	\N
49	Lo Espejo 4567, Lo Espejo	2025-08-22 21:52:05.565246	hector.flores@gmail.com	Héctor Flores Vargas	FATHER	+56987111126	Pintor	11111141-3	\N
51	Independencia 5678, Independencia	2025-08-22 21:52:05.565246	ricardo.contreras@gmail.com	Ricardo Contreras Muñoz	FATHER	+56987111127	Técnico	11111143-K	\N
53	Recoleta 6789, Recoleta	2025-08-22 21:52:05.565246	enrique.parra@gmail.com	Enrique Parra Torres	FATHER	+56987111128	Gásfiter	11111145-6	\N
55	Conchalí 7890, Conchalí	2025-08-22 21:52:05.565246	ramon.aguilar@gmail.com	Ramón Aguilar González	FATHER	+56987111129	Conserje	11111147-2	\N
57	Huechuraba 8901, Huechuraba	2025-08-22 21:52:05.565246	ivan.fuentes@gmail.com	Iván Fuentes Hernández	FATHER	+56987111130	Bodeguero	11111149-9	\N
59	Las Condes 1234	2025-08-22 21:55:50.934166	carlos.gonzalez@test.cl	Carlos González Hernández	FATHER	+56921000001	Ingeniero Civil	40003001-2	\N
60	Las Condes 1234	2025-08-22 21:55:50.934166	familia01@test.cl	María José González Silva	MOTHER	+56911000001	Profesora	40003001-3	\N
61	Providencia 567	2025-08-22 21:55:50.934166	pedro.rodriguez@test.cl	Pedro Rodríguez Morales	FATHER	+56921000002	Contador	40003002-0	\N
62	Providencia 567	2025-08-22 21:55:50.934166	familia02@test.cl	Carmen Rodríguez López	MOTHER	+56911000002	Enfermera	40003002-1	\N
63	Ñuñoa 890	2025-08-22 21:55:50.934166	luis.morales@test.cl	Luis Morales Pérez	FATHER	+56921000003	Médico	40003003-9	\N
64	Ñuñoa 890	2025-08-22 21:55:50.934166	familia03@test.cl	Patricia Morales Castro	MOTHER	+56911000003	Psicóloga	40003003-K	\N
65	La Reina 2345	2025-08-22 21:55:50.934166	roberto.silva@test.cl	Roberto Silva Vargas	FATHER	+56921000004	Arquitecto	40003004-7	\N
66	La Reina 2345	2025-08-22 21:55:50.934166	familia04@test.cl	Andrea Silva Torres	MOTHER	+56911000004	Diseñadora	40003004-8	\N
67	Vitacura 3456	2025-08-22 21:55:50.934166	miguel.hernandez@test.cl	Miguel Hernández Silva	FATHER	+56921000005	Abogado	40003005-5	\N
68	Vitacura 3456	2025-08-22 21:55:50.934166	familia05@test.cl	Claudia Hernández Pérez	MOTHER	+56911000005	Kinesióloga	40003005-6	\N
69	Peñalolén 4567	2025-08-22 21:55:50.934166	jorge.castro@test.cl	Jorge Castro Torres	FATHER	+56921000006	Veterinario	40003006-3	\N
70	Peñalolén 4567	2025-08-22 21:55:50.934166	familia06@test.cl	Valeria Castro Morales	MOTHER	+56911000006	Nutricionista	40003006-4	\N
71	La Florida 5678	2025-08-22 21:55:50.934166	andres.vargas@test.cl	Andrés Vargas González	FATHER	+56921000007	Periodista	40003007-1	\N
72	La Florida 5678	2025-08-22 21:55:50.934166	familia07@test.cl	Mónica Vargas Silva	MOTHER	+56911000007	Traductora	40003007-2	\N
73	Maipú 6789	2025-08-22 21:55:50.934166	felipe.munoz@test.cl	Felipe Muñoz Hernández	FATHER	+56921000008	Dentista	40003008-K	\N
74	Maipú 6789	2025-08-22 21:55:50.934166	familia08@test.cl	Francisca Muñoz González	MOTHER	+56911000008	Fonoaudióloga	40003008-0	\N
75	San Miguel 7890	2025-08-22 21:55:50.934166	patricio.torres@test.cl	Patricio Torres Castro	FATHER	+56921000009	Electricista	40003009-8	\N
76	San Miguel 7890	2025-08-22 21:55:50.934166	familia09@test.cl	Soledad Torres Hernández	MOTHER	+56911000009	Secretaria	40003009-9	\N
77	Quilicura 8901	2025-08-22 21:55:50.934166	rodrigo.perez@test.cl	Rodrigo Pérez Vargas	FATHER	+56921000010	Mecánico	40003010-2	\N
78	Quilicura 8901	2025-08-22 21:55:50.934166	familia10@test.cl	Alejandra Pérez Castro	MOTHER	+56911000010	Técnico en Párvulos	40003010-3	\N
79	Puente Alto 9012	2025-08-22 21:55:50.934166	cristian.lopez@test.cl	Cristián López Muñoz	FATHER	+56921000011	Profesor	40003011-0	\N
80	Puente Alto 9012	2025-08-22 21:55:50.934166	familia11@test.cl	Lorena López Vargas	MOTHER	+56911000011	Administradora	40003011-1	\N
81	Renca 0123	2025-08-22 21:55:50.934166	gonzalo.garcia@test.cl	Gonzalo García Torres	FATHER	+56921000012	Soldador	40003012-9	\N
82	Renca 0123	2025-08-22 21:55:50.934166	familia12@test.cl	Paola García Muñoz	MOTHER	+56911000012	Cajera	40003012-K	\N
83	Cerro Navia 1234	2025-08-22 21:55:50.934166	fernando.martinez@test.cl	Fernando Martínez González	FATHER	+56921000013	Carpintero	40003013-7	\N
84	Cerro Navia 1234	2025-08-22 21:55:50.934166	familia13@test.cl	Verónica Martínez Torres	MOTHER	+56911000013	Auxiliar de Enfermería	40003013-8	\N
85	Estación Central 2345	2025-08-22 21:55:50.934166	mauricio.sanchez@test.cl	Mauricio Sánchez Hernández	FATHER	+56921000014	Guardia	40003014-5	\N
86	Estación Central 2345	2025-08-22 21:55:50.934166	familia14@test.cl	Carolina Sánchez Pérez	MOTHER	+56911000014	Vendedora	40003014-6	\N
87	Pedro Aguirre Cerda 3456	2025-08-22 21:55:50.934166	osvaldo.ramos@test.cl	Osvaldo Ramos Castro	FATHER	+56921000015	Conductor	40003015-3	\N
88	Pedro Aguirre Cerda 3456	2025-08-22 21:55:50.934166	familia15@test.cl	Daniela Ramos López	MOTHER	+56911000015	Operaria	40003015-4	\N
89	Lo Espejo 4567	2025-08-22 21:55:50.934166	hector.flores@test.cl	Héctor Flores Vargas	FATHER	+56921000016	Pintor	40003016-1	\N
90	Lo Espejo 4567	2025-08-22 21:55:50.934166	familia16@test.cl	Marcela Flores García	MOTHER	+56911000016	Asesora del Hogar	40003016-2	\N
91	Independencia 5678	2025-08-22 21:55:50.934166	ricardo.contreras@test.cl	Ricardo Contreras Muñoz	FATHER	+56921000017	Técnico	40003017-K	\N
92	Independencia 5678	2025-08-22 21:55:50.934166	familia17@test.cl	Gladys Contreras Martínez	MOTHER	+56911000017	Costurera	40003017-0	\N
93	Recoleta 6789	2025-08-22 21:55:50.934166	enrique.parra@test.cl	Enrique Parra Torres	FATHER	+56921000018	Gásfiter	40003018-8	\N
94	Recoleta 6789	2025-08-22 21:55:50.934166	familia18@test.cl	Cecilia Parra Sánchez	MOTHER	+56911000018	Peluquera	40003018-9	\N
95	Conchalí 7890	2025-08-22 21:55:50.934166	ramon.aguilar@test.cl	Ramón Aguilar González	FATHER	+56921000019	Conserje	40003019-6	\N
96	Conchalí 7890	2025-08-22 21:55:50.934166	familia19@test.cl	Roxana Aguilar Ramos	MOTHER	+56911000019	Manipuladora de Alimentos	40003019-7	\N
97	Huechuraba 8901	2025-08-22 21:55:50.934166	ivan.fuentes@test.cl	Iván Fuentes Hernández	FATHER	+56921000020	Bodeguero	40003020-0	\N
98	Huechuraba 8901	2025-08-22 21:55:50.934166	familia20@test.cl	Ingrid Fuentes Flores	MOTHER	+56911000020	Auxiliar de Aseo	40003020-1	\N
\.


--
-- Data for Name: problemas; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.problemas (id, codigo_inicial, descripcion, dificultad, solucion_correcta, test_cases_json, titulo, tema_id) FROM stdin;
\.


--
-- Data for Name: professor_grades; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.professor_grades (professor_id, grade) FROM stdin;
\.


--
-- Data for Name: professor_qualifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.professor_qualifications (professor_id, qualification) FROM stdin;
\.


--
-- Data for Name: professor_subjects; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.professor_subjects (professor_id, subject) FROM stdin;
\.


--
-- Data for Name: professors; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.professors (id, department, is_admin, years_of_experience) FROM stdin;
\.


--
-- Data for Name: progreso_usuario; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.progreso_usuario (id, estado, intentos, ultima_modificacion, problema_id, usuario_id) FROM stdin;
\.


--
-- Data for Name: psychologist_grades; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.psychologist_grades (psychologist_id, grade) FROM stdin;
\.


--
-- Data for Name: psychologist_specialized_areas; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.psychologist_specialized_areas (psychologist_id, area) FROM stdin;
\.


--
-- Data for Name: psychologists; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.psychologists (id, can_conduct_interviews, can_perform_psychological_evaluations, license_number, specialty) FROM stdin;
\.


--
-- Data for Name: ranking; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ranking (id, fecha_actualizacion, puntaje, usuario_id) FROM stdin;
\.


--
-- Data for Name: students; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.students (id, additional_notes, address, birth_date, created_at, current_school, email, first_name, grade_applied, maternal_last_name, paternal_last_name, rut, school_applied, updated_at, is_employee_child, is_alumni_child, is_inclusion_student, employee_parent_name, alumni_parent_year, inclusion_type, inclusion_notes, age, target_school) FROM stdin;
8	\N	Maipú 3789, Maipú	2013-06-18	2025-08-22 18:36:58.604591	Escuela Básica Santa María	\N	Isidora Paz	5° Básico	Hernández	Vargas	31789012-3	\N	\N	f	t	f	\N	2005	\N	\N	12	MONTE_TABOR
9	\N	Irarrázaval 5678, Macul	2012-01-27	2025-08-22 18:36:58.604591	Colegio Monte Verde	\N	Cristóbal Andrés	6° Básico	Vargas	Sánchez	32890123-4	\N	\N	f	t	t	\N	2005	Síndrome de Down	Requiere apoyo en lectoescritura y matemáticas básicas	13	MONTE_TABOR
18	\N	La Florida 5678, La Florida	2019-09-18	2025-08-22 21:52:05.562766	Jardín María Montessori	\N	Isidora	Kinder	Silva	Vargas	25000003-0	NAZARET	\N	f	t	f	\N	2010	\N	\N	5	MONTE_TABOR
14	\N	Ñuñoa 890, Ñuñoa	2020-11-08	2025-08-22 21:52:05.562766	Jardín Los Patitos	\N	Valentina	Prekinder	Castro	Morales	26000003-8	MONTE_TABOR	\N	f	f	t	\N	\N	TDAH (Trastorno por Déficit de Atención)	Necesita estrategias de concentración y pausas frecuentes	4	MONTE_TABOR
19	\N	Maipú 6789, Maipú	2019-12-03	2025-08-22 21:52:05.562766	Jardín Los Pequitos	\N	Agustín	Kinder	González	Muñoz	25000004-9	NAZARET	\N	f	f	t	\N	\N	Discapacidad Visual Parcial	Requiere material en formato grande y ubicación preferencial	5	MONTE_TABOR
11	\N	Gran Avenida 4567, San Miguel	2010-10-30	2025-08-22 18:36:58.604591	Liceo Municipal San Carlos	\N	Gabriel Eduardo	8° Básico	Espinoza	Contreras	34012345-6	\N	\N	t	f	f	Carlos Moreno - Director Académico	\N	\N	\N	14	MONTE_TABOR
12	\N	Las Condes 1234, Las Condes	2020-03-15	2025-08-22 21:52:05.562766	Jardín Los Angelitos	\N	Emilia	Prekinder	Silva	González	26000001-1	MONTE_TABOR	\N	t	f	f	Carlos Moreno - Director Académico	\N	\N	\N	5	MONTE_TABOR
16	\N	Vitacura 3456, Vitacura	2019-02-14	2025-08-22 21:52:05.562766	Jardín Los Rosales	\N	Sofía	Kinder	Pérez	Hernández	25000001-4	NAZARET	\N	t	f	f	Patricia López - Profesora de Inglés	\N	\N	\N	6	MONTE_TABOR
10	\N	Vitacura 9012, Vitacura	2011-08-14	2025-08-22 18:36:58.604591	Colegio Sagrado Corazón	\N	Magdalena Esperanza	7° Básico	Jiménez	Espinoza	33901234-5	\N	\N	f	f	f	\N	\N	\N	\N	14	MONTE_TABOR
15	\N	La Reina 2345, La Reina	2020-05-12	2025-08-22 21:52:05.562766	\N	\N	Benjamín	Prekinder	Torres	Silva	26000004-6	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	5	MONTE_TABOR
17	\N	Peñalolén 4567, Peñalolén	2019-06-30	2025-08-22 21:52:05.562766	Jardín San Francisco	\N	Diego	Kinder	Morales	Castro	25000002-2	NAZARET	\N	f	f	f	\N	\N	\N	\N	6	MONTE_TABOR
20	\N	San Miguel 7890, San Miguel	2018-01-20	2025-08-22 21:52:05.562766	Colegio San José	\N	Martina	1° Básico	Hernández	Torres	24000001-7	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	7	MONTE_TABOR
21	\N	Quilicura 8901, Quilicura	2017-04-25	2025-08-22 21:52:05.562766	Escuela Los Pinos	\N	Lucas	2° Básico	Castro	Pérez	23000001-0	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	8	MONTE_TABOR
22	\N	Puente Alto 9012, Puente Alto	2016-08-10	2025-08-22 21:52:05.562766	Colegio Santa Teresa	\N	Antonella	3° Básico	Vargas	López	22000001-3	NAZARET	\N	f	f	f	\N	\N	\N	\N	9	NAZARET
23	\N	Renca 0123, Renca	2015-11-15	2025-08-22 21:52:05.562766	Escuela República	\N	Tomás	4° Básico	Muñoz	García	21000001-6	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	9	NAZARET
24	\N	Cerro Navia 1234, Cerro Navia	2014-03-08	2025-08-22 21:52:05.562766	Colegio Los Andes	\N	Florencia	5° Básico	Torres	Martínez	20000001-9	NAZARET	\N	f	f	f	\N	\N	\N	\N	11	MONTE_TABOR
25	\N	Estación Central 2345, Estación Central	2013-07-12	2025-08-22 21:52:05.562766	Escuela El Bosque	\N	Sebastián	6° Básico	Pérez	Sánchez	19000001-2	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	12	MONTE_TABOR
26	\N	Pedro Aguirre Cerda 3456, Pedro Aguirre Cerda	2012-10-22	2025-08-22 21:52:05.562766	Colegio San Pablo	\N	Catalina	7° Básico	López	Ramos	18000001-5	NAZARET	\N	f	f	f	\N	\N	\N	\N	12	MONTE_TABOR
27	\N	Lo Espejo 4567, Lo Espejo	2011-12-30	2025-08-22 21:52:05.562766	Escuela Gabriela Mistral	\N	Maximiliano	8° Básico	García	Flores	17000001-8	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	13	MONTE_TABOR
28	\N	Independencia 5678, Independencia	2010-02-18	2025-08-22 21:52:05.562766	Liceo Pablo Neruda	\N	Javiera	I° Medio	Martínez	Contreras	16000001-1	NAZARET	\N	f	f	f	\N	\N	\N	\N	15	MONTE_TABOR
29	\N	Recoleta 6789, Recoleta	2009-05-14	2025-08-22 21:52:05.562766	Colegio Industrial	\N	Nicolás	II° Medio	Sánchez	Parra	15000001-4	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	16	MONTE_TABOR
30	\N	Conchalí 7890, Conchalí	2008-09-26	2025-08-22 21:52:05.562766	Liceo de Niñas N°1	\N	Fernanda	III° Medio	Ramos	Aguilar	14000001-7	NAZARET	\N	f	f	f	\N	\N	\N	\N	16	MONTE_TABOR
31	\N	Huechuraba 8901, Huechuraba	2007-11-11	2025-08-22 21:52:05.562766	Liceo Comercial	\N	Francisco	IV° Medio	Flores	Fuentes	13000001-0	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	17	MONTE_TABOR
32	\N	Las Condes 1234	2020-03-15	2025-08-22 21:55:50.933213	Jardín Los Angelitos	\N	Emilia	Prekinder	Silva	González	40002001-0	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	5	MONTE_TABOR
33	\N	Providencia 567	2020-07-22	2025-08-22 21:55:50.933213	Jardín Santa María	\N	Matías	Prekinder	López	Rodríguez	40002002-9	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	5	MONTE_TABOR
34	\N	Ñuñoa 890	2020-11-08	2025-08-22 21:55:50.933213	Jardín Los Patitos	\N	Valentina	Prekinder	Castro	Morales	40002003-7	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	4	MONTE_TABOR
35	\N	La Reina 2345	2020-05-12	2025-08-22 21:55:50.933213	\N	\N	Benjamín	Prekinder	Torres	Silva	40002004-5	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	5	MONTE_TABOR
36	\N	Vitacura 3456	2019-02-14	2025-08-22 21:55:50.933213	Jardín Los Rosales	\N	Sofía	Kinder	Pérez	Hernández	40002005-3	NAZARET	\N	f	f	f	\N	\N	\N	\N	6	MONTE_TABOR
37	\N	Peñalolén 4567	2019-06-30	2025-08-22 21:55:50.933213	Jardín San Francisco	\N	Diego	Kinder	Morales	Castro	40002006-1	NAZARET	\N	f	f	f	\N	\N	\N	\N	6	MONTE_TABOR
38	\N	La Florida 5678	2019-09-18	2025-08-22 21:55:50.933213	Jardín María Montessori	\N	Isidora	Kinder	Silva	Vargas	40002007-K	NAZARET	\N	f	f	f	\N	\N	\N	\N	5	MONTE_TABOR
39	\N	Maipú 6789	2019-12-03	2025-08-22 21:55:50.933213	Jardín Los Pequitos	\N	Agustín	Kinder	González	Muñoz	40002008-8	NAZARET	\N	f	f	f	\N	\N	\N	\N	5	MONTE_TABOR
40	\N	San Miguel 7890	2018-01-20	2025-08-22 21:55:50.933213	Colegio San José	\N	Martina	1° Básico	Hernández	Torres	40002009-6	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	7	MONTE_TABOR
41	\N	Quilicura 8901	2017-04-25	2025-08-22 21:55:50.933213	Escuela Los Pinos	\N	Lucas	2° Básico	Castro	Pérez	40002010-K	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	8	MONTE_TABOR
42	\N	Puente Alto 9012	2016-08-10	2025-08-22 21:55:50.933213	Colegio Santa Teresa	\N	Antonella	3° Básico	Vargas	López	40002011-8	NAZARET	\N	f	f	f	\N	\N	\N	\N	9	NAZARET
43	\N	Renca 0123	2015-11-15	2025-08-22 21:55:50.933213	Escuela República	\N	Tomás	4° Básico	Muñoz	García	40002012-6	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	9	NAZARET
13	\N	Providencia 567, Providencia	2020-07-22	2025-08-22 21:52:05.562766	Jardín Santa María	\N	Matías	Prekinder	López	Rodríguez	26000002-K	MONTE_TABOR	\N	f	t	f	\N	1995	\N	\N	5	MONTE_TABOR
2	\N	Av. Las Condes 1234, Las Condes	2019-03-15	2025-08-22 18:36:58.604591	\N	\N	Sofía Isabella	Prekinder	Pérez	González	25123456-7	\N	\N	t	f	f	María González - Secretaria Académica	\N	\N	\N	6	MONTE_TABOR
3	\N	Calle Los Robles 567, Providencia	2018-07-22	2025-08-22 18:36:58.604591	Jardín Los Angelitos	\N	Mateo Alejandro	Kinder	Morales	Pérez	26234567-8	\N	\N	f	t	f	\N	1998	\N	\N	7	MONTE_TABOR
4	\N	Pasaje San Martín 890, Ñuñoa	2017-11-08	2025-08-22 18:36:58.604591	Colegio San José	\N	Valentina María	1° Básico	Silva	Rodríguez	27345678-9	\N	\N	f	t	f	\N	1998	\N	\N	7	MONTE_TABOR
6	\N	Los Pinos 678, La Reina	2015-09-25	2025-08-22 18:36:58.604591	Colegio Particular San Pablo	\N	Antonella Fernanda	3° Básico	González	Morales	29567890-1	\N	\N	f	f	t	\N	\N	TEA (Trastorno del Espectro Autista)	Requiere apoyo especializado en comunicación y adaptaciones curriculares	9	NAZARET
7	\N	Santa Rosa 1456, Santiago Centro	2014-12-03	2025-08-22 18:36:58.604591	Colegio Los Pinos	\N	Benjamín Ignacio	4° Básico	Torres	Jiménez	30678901-2	\N	\N	f	f	t	\N	\N	TEA (Trastorno del Espectro Autista)	Requiere apoyo especializado en comunicación y adaptaciones curriculares	10	NAZARET
44	\N	Cerro Navia 1234	2014-03-08	2025-08-22 21:55:50.933213	Colegio Los Andes	\N	Florencia	5° Básico	Torres	Martínez	40002013-4	NAZARET	\N	f	f	f	\N	\N	\N	\N	11	MONTE_TABOR
45	\N	Estación Central 2345	2013-07-12	2025-08-22 21:55:50.933213	Escuela El Bosque	\N	Sebastián	6° Básico	Pérez	Sánchez	40002014-2	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	12	MONTE_TABOR
46	\N	Pedro Aguirre Cerda 3456	2012-10-22	2025-08-22 21:55:50.933213	Colegio San Pablo	\N	Catalina	7° Básico	López	Ramos	40002015-0	NAZARET	\N	f	f	f	\N	\N	\N	\N	12	MONTE_TABOR
47	\N	Lo Espejo 4567	2011-12-30	2025-08-22 21:55:50.933213	Escuela Gabriela Mistral	\N	Maximiliano	8° Básico	García	Flores	40002016-9	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	13	MONTE_TABOR
48	\N	Independencia 5678	2010-02-18	2025-08-22 21:55:50.933213	Liceo Pablo Neruda	\N	Javiera	I° Medio	Martínez	Contreras	40002017-7	NAZARET	\N	f	f	f	\N	\N	\N	\N	15	MONTE_TABOR
49	\N	Recoleta 6789	2009-05-14	2025-08-22 21:55:50.933213	Colegio Industrial	\N	Nicolás	II° Medio	Sánchez	Parra	40002018-5	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	16	MONTE_TABOR
50	\N	Conchalí 7890	2008-09-26	2025-08-22 21:55:50.933213	Liceo de Niñas N°1	\N	Fernanda	III° Medio	Ramos	Aguilar	40002019-3	NAZARET	\N	f	f	f	\N	\N	\N	\N	16	MONTE_TABOR
51	\N	Huechuraba 8901	2007-11-11	2025-08-22 21:55:50.933213	Liceo Comercial	\N	Francisco	IV° Medio	Flores	Fuentes	40002020-7	MONTE_TABOR	\N	f	f	f	\N	\N	\N	\N	17	MONTE_TABOR
1	ESTUDIANTE DESTACADO EN MATEMÁTICAS	AV. PROVIDENCIA 1234, SANTIAGO	2015-03-15	2025-08-22 04:00:20.709543	COLEGIO SAN IGNACIO	juan.gonzalez@mail.com	JUAN CARLOS	3° BÁSICO	PÉREZ	GONZÁLEZ	12345678-9	MONTE_TABOR	2025-08-22 04:00:20.709543	t	f	f	María González - Secretaria Académica	\N	\N	\N	10	MONTE_TABOR
5	\N	Av. Grecia 2345, Peñalolén	2016-04-12	2025-08-22 18:36:58.604591	Escuela Municipal Las Flores	\N	Diego Sebastián	2° Básico	López	Hernández	28456789-0	\N	\N	f	t	f	\N	1998	\N	\N	9	MONTE_TABOR
\.


--
-- Data for Name: students_backup; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.students_backup (id, additional_notes, address, birth_date, created_at, current_school, email, first_name, grade_applied, rut, updated_at, paternal_last_name, maternal_last_name, school_applied) FROM stdin;
\.


--
-- Data for Name: support_staff; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.support_staff (id, can_access_reports, can_manage_schedules, department, staff_type) FROM stdin;
\.


--
-- Data for Name: support_staff_responsibilities; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.support_staff_responsibilities (staff_id, responsibility) FROM stdin;
\.


--
-- Data for Name: supporters; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.supporters (id, created_at, email, full_name, phone, relationship, rut, updated_at) FROM stdin;
3	2025-08-17 17:48:51.653795	jorge.gangale@gmail.com	jorge gangale	+56983620169	PADRE	11.650.825-7	2025-08-17 17:48:51.653806
4	2025-08-17 21:05:56.213671	jorge.gangale@gmail.com	jorge gangale	+56983621068	PADRE	11.650.825-7	2025-08-17 21:05:56.213687
5	2025-08-17 22:21:23.832885	carlos.perez@email.com	Carlos Pérez	+56912345678	PADRE	11111111-1	2025-08-17 22:21:23.832891
6	2025-08-17 22:43:44.169249	jorge.gangale@gmail.com	jorge gangale	+56983621068	PADRE	11.650.825-7	2025-08-17 22:43:44.169261
7	2025-08-17 22:52:19.353019	roberto.martinez@email.com	Roberto Martinez	+56911223344	PADRE	15555555-5	2025-08-17 22:52:19.353025
8	2025-08-18 19:43:40.358344	schweikart.cr@gmail.com	cristian schweikart	+56987654321	PADRE	18.879.517-K	2025-08-18 19:43:40.358346
9	2025-08-22 04:00:20.717173	carlos.gonzalez@mail.com	CARLOS GONZÁLEZ MARTÍNEZ	+56987654321	PADRE	87654321-0	2025-08-22 04:00:20.717173
12	2025-08-23 02:44:47.132309	carlos.gonzalez@test.cl	Carlos González Hernández	+56921000001	PADRE	40003001-2	\N
13	2025-08-23 02:44:47.132309	pedro.rodriguez@test.cl	Pedro Rodríguez Morales	+56921000002	PADRE	40003002-0	\N
14	2025-08-23 02:44:47.132309	luis.morales@test.cl	Luis Morales Pérez	+56921000003	PADRE	40003003-9	\N
15	2025-08-23 02:44:47.132309	roberto.silva@test.cl	Roberto Silva Vargas	+56921000004	PADRE	40003004-7	\N
16	2025-08-23 02:44:47.132309	miguel.hernandez@test.cl	Miguel Hernández Silva	+56921000005	PADRE	40003005-5	\N
17	2025-08-23 02:44:47.132309	jorge.castro@test.cl	Jorge Castro Torres	+56921000006	PADRE	40003006-3	\N
18	2025-08-23 02:44:47.132309	andres.vargas@test.cl	Andrés Vargas González	+56921000007	PADRE	40003007-1	\N
19	2025-08-23 02:44:47.132309	felipe.munoz@test.cl	Felipe Muñoz Hernández	+56921000008	PADRE	40003008-K	\N
20	2025-08-23 02:44:47.132309	patricio.torres@test.cl	Patricio Torres Castro	+56921000009	PADRE	40003009-8	\N
21	2025-08-23 02:44:47.132309	rodrigo.perez@test.cl	Rodrigo Pérez Vargas	+56921000010	PADRE	40003010-2	\N
22	2025-08-23 02:44:47.132309	cristian.lopez@test.cl	Cristián López Muñoz	+56921000011	PADRE	40003011-0	\N
23	2025-08-23 02:44:47.132309	gonzalo.garcia@test.cl	Gonzalo García Torres	+56921000012	PADRE	40003012-9	\N
24	2025-08-23 02:44:47.132309	fernando.martinez@test.cl	Fernando Martínez González	+56921000013	PADRE	40003013-7	\N
25	2025-08-23 02:44:47.132309	mauricio.sanchez@test.cl	Mauricio Sánchez Hernández	+56921000014	PADRE	40003014-5	\N
26	2025-08-23 02:44:47.132309	osvaldo.ramos@test.cl	Osvaldo Ramos Castro	+56921000015	PADRE	40003015-3	\N
27	2025-08-23 02:44:47.132309	hector.flores@test.cl	Héctor Flores Vargas	+56921000016	PADRE	40003016-1	\N
28	2025-08-23 02:44:47.132309	ricardo.contreras@test.cl	Ricardo Contreras Muñoz	+56921000017	PADRE	40003017-K	\N
29	2025-08-23 02:44:47.132309	enrique.parra@test.cl	Enrique Parra Torres	+56921000018	PADRE	40003018-8	\N
30	2025-08-23 02:44:47.132309	ramon.aguilar@test.cl	Ramón Aguilar González	+56921000019	PADRE	40003019-6	\N
31	2025-08-23 02:44:47.132309	ivan.fuentes@test.cl	Iván Fuentes Hernández	+56921000020	PADRE	40003020-0	\N
\.


--
-- Data for Name: temas; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.temas (id, descripcion, dificultad, nombre) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, active, created_at, email, email_verified, first_name, last_name, password, phone, role, rut, updated_at, educational_level, subject) FROM stdin;
17	t	2025-08-18 19:38:23.959099	schweikart.cr@gmail.com	t	cristian	schweikart	$2a$10$GvTYjC7BJSo0j2RSyvQ6n.lk1L4DCyqoTCjjd3743QQ4qs1aoWs16	+56987654321	APODERADO	18.879.517-K	\N	\N	\N
3	t	2025-08-17 17:46:06.625224	jorge.gangale@gmail.com	t	jorge	gangale	a/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56983620169	APODERADO	11.650.825-7	\N	\N	\N
32	t	2025-08-19 23:40:52.76469	carmen.sanchez@mtn.cl	t	Carmen	Sánchez	$2a$10$cS5X8ui.zUZJneWpRyBxSuzcYU887m1FY4CK/DxQ3cmywiE/XEq5G	+56 9 7890 1234	CYCLE_DIRECTOR	78.901.234-5	2025-08-20 08:28:07.720288	\N	\N
43	t	2025-08-20 21:53:05.743819	diego.moreno@mtn.cl	t	Diego	Moreno Aguirre	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56912345618	PSYCHOLOGIST	12345679-7	\N	\N	\N
33	t	2025-08-19 23:40:52.76469	ricardo.morales@mtn.cl	t	Ricardo	Morales	$2a$10$cS5X8ui.zUZJneWpRyBxSuzcYU887m1FY4CK/DxQ3cmywiE/XEq5G	+56 9 8901 2345	CYCLE_DIRECTOR	89012345-6	2025-08-19 23:40:52.76469	\N	\N
34	t	2025-08-19 23:40:52.76563	laura.fernandez@mtn.cl	t	Laura	Fernández	$2a$10$cS5X8ui.zUZJneWpRyBxSuzcYU887m1FY4CK/DxQ3cmywiE/XEq5G	+56 9 9012 3456	PSYCHOLOGIST	90123456-7	2025-08-19 23:40:52.76563	\N	\N
35	t	2025-08-19 23:40:52.76563	andres.silva@mtn.cl	t	Andrés	Silva	$2a$10$cS5X8ui.zUZJneWpRyBxSuzcYU887m1FY4CK/DxQ3cmywiE/XEq5G	+56 9 0123 4567	PSYCHOLOGIST	01234567-8	2025-08-19 23:40:52.76563	\N	\N
52	t	2025-08-20 22:30:14.266778	ana.castillo@mtn.cl	t	Ana María	Castillo Ruiz	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56912345607	PSYCHOLOGIST	12345678-6	\N	\N	\N
53	t	2025-08-20 22:30:14.267326	sebastian.campos@mtn.cl	t	Sebastián	Campos Olivares	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56912345608	CYCLE_DIRECTOR	12345678-7	\N	\N	\N
30	t	2025-08-19 23:40:52.763738	jennifer.smith@mtn.cl	t	Jennifer	Smith	$2a$10$cS5X8ui.zUZJneWpRyBxSuzcYU887m1FY4CK/DxQ3cmywiE/XEq5G	+56 9 5678 9012	TEACHER	56789012-3	2025-08-19 23:40:52.763738	\N	\N
36	t	2025-08-19 23:41:26.431875	maria.gonzalez@mtn.cl	t	María	González	$2a$10$cS5X8ui.zUZJneWpRyBxSuzcYU887m1FY4CK/DxQ3cmywiE/XEq5G	+56 9 1234 5678	TEACHER	15234678-9	2025-08-19 23:41:26.431875	\N	\N
54	t	2025-08-20 22:58:42.630456	maria.nueva@mtn.cl	t	María Elena	González	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911111111	TEACHER	11111111-1	\N	PRESCHOOL	GENERAL
55	t	2025-08-20 22:58:42.630456	roberto.nuevo@mtn.cl	t	Roberto	Silva	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56922222222	TEACHER	22222222-2	\N	BASIC	MATHEMATICS
56	t	2025-08-20 22:58:42.630456	carmen.nueva@mtn.cl	t	Carmen	Morales	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56933333333	TEACHER	33333333-3	\N	HIGH_SCHOOL	LANGUAGE
57	t	2025-08-20 22:58:42.630456	marcela.coord@mtn.cl	t	Marcela	Coordinadora	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56944444444	COORDINATOR	44444444-4	\N	ALL_LEVELS	MATHEMATICS
59	t	2025-08-20 22:32:49.772959	departamento.matematica@mtn.cl	t	Jorge	Hernandez	$2a$10$sVbY7RBvfdqKkkDI6CfCJ.4jtUFpyW4aRv/E8Im2UHy.D8eI5JnG.	+56 9 8362 0169	TEACHER	24.137.502-1	\N	HIGH_SCHOOL	MATHEMATICS
71	t	2025-08-22 12:18:15.343253	test.admin@mtn.cl	t	Test	Admin	$2a$10$bqjQzuMlbjM74VdKgb5M2u/qTEBNzKOXPlXVDKaHoMB6ujDmPmMUa	+56988888888	ADMIN	88888888-8	\N	ALL_LEVELS	\N
4	t	2025-08-17 22:20:39.38084	test.user@mtn.cl	t	Test	User	$2b$12$BGJbLj.E1ORHCjqrtGQEJuGRhC40fvUxMps5z4MqC4Ji8oBCc9yqO	+56912345678	APODERADO	11.111.111-1	\N	\N	\N
66	t	2025-08-22 03:58:27.558475	test.apoderado@mtn.cl	t	Test	Apoderado	$2b$12$Vc3vdAeUax.VJDy05UKFDuaDVvh0WuXPztolhQjwgdkCMBMmNkp.S	+56911111111	APODERADO	11111111-K	\N	\N	\N
26	t	2025-08-19 19:13:39.501247	alejandra.flores@mtn.cl	t	alejandra	flores	$2b$12$Y474ipoiCqcT8FdAb4BsFefv8V9gYZ0aZMaJliHv8mDQZZpNZI1Am	+56 9 8362 0169	TEACHER	12.265.495-8	\N	\N	\N
67	t	2025-08-22 12:02:28.616267	admin@mtn.cl	t	Admin	Sistema	$2a$10$bqjQzuMlbjM74VdKgb5M2u/qTEBNzKOXPlXVDKaHoMB6ujDmPmMUa	+56900000000	ADMIN	10000000-0	\N	\N	\N
72	t	2025-08-22 18:35:31.132433	ana.gonzalez@gmail.com	t	Ana María	González López	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56987654321	APODERADO	15123456-7	\N	\N	\N
73	t	2025-08-22 18:35:31.132433	carlos.perez@hotmail.com	t	Carlos Eduardo	Pérez Silva	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56912345678	APODERADO	16234567-8	\N	\N	\N
25	t	2025-08-19 23:02:31.611202	jorge.gangale@mtn.cl	t	Jorge	Gangale	$2a$10$Lc1X60TC3wBdxyIu0OZD9OZFRF.9ClEaGSyLHl4ex6ObG0.kgTBAK	\N	ADMIN	12345678-9	\N	\N	\N
74	t	2025-08-22 18:35:31.132433	maria.rodriguez@yahoo.com	t	María Isabel	Rodríguez Torres	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56923456789	APODERADO	17345678-9	\N	\N	\N
75	t	2025-08-22 18:35:31.132433	jose.hernandez@gmail.com	t	José Miguel	Hernández Castro	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56934567890	APODERADO	18456789-0	\N	\N	\N
76	t	2025-08-22 18:35:31.132433	patricia.morales@outlook.com	t	Patricia Elena	Morales Vega	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56945678901	APODERADO	19567890-1	\N	\N	\N
77	t	2025-08-22 18:35:31.132433	roberto.jimenez@gmail.com	t	Roberto Carlos	Jiménez Flores	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56956789012	APODERADO	20678901-2	\N	\N	\N
78	t	2025-08-22 18:35:31.132433	francisca.vargas@yahoo.com	t	Francisca Andrea	Vargas Mendoza	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56967890123	APODERADO	21789012-3	\N	\N	\N
79	t	2025-08-22 18:35:31.132433	luis.sanchez@hotmail.com	t	Luis Fernando	Sánchez Rojas	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56978901234	APODERADO	22890123-4	\N	\N	\N
80	t	2025-08-22 18:35:31.132433	carmen.espinoza@gmail.com	t	Carmen Gloria	Espinoza Núñez	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56989012345	APODERADO	23901234-5	\N	\N	\N
81	t	2025-08-22 18:35:31.132433	alejandro.contreras@outlook.com	t	Alejandro José	Contreras Pinto	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56990123456	APODERADO	24012345-6	\N	\N	\N
84	t	2025-08-22 21:55:50.932301	familia01@test.cl	t	María José	González	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000001	APODERADO	40001000-1	\N	\N	\N
85	t	2025-08-22 21:55:50.932301	familia02@test.cl	t	Carmen	Rodríguez	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000002	APODERADO	40001000-2	\N	\N	\N
86	t	2025-08-22 21:55:50.932301	familia03@test.cl	t	Patricia	Morales	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000003	APODERADO	40001000-3	\N	\N	\N
87	t	2025-08-22 21:55:50.932301	familia04@test.cl	t	Andrea	Silva	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000004	APODERADO	40001000-4	\N	\N	\N
88	t	2025-08-22 21:55:50.932301	familia05@test.cl	t	Claudia	Hernández	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000005	APODERADO	40001000-5	\N	\N	\N
89	t	2025-08-22 21:55:50.932301	familia06@test.cl	t	Valeria	Castro	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000006	APODERADO	40001000-6	\N	\N	\N
90	t	2025-08-22 21:55:50.932301	familia07@test.cl	t	Mónica	Vargas	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000007	APODERADO	40001000-7	\N	\N	\N
91	t	2025-08-22 21:55:50.932301	familia08@test.cl	t	Francisca	Muñoz	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000008	APODERADO	40001000-8	\N	\N	\N
92	t	2025-08-22 21:55:50.932301	familia09@test.cl	t	Soledad	Torres	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000009	APODERADO	40001000-9	\N	\N	\N
93	t	2025-08-22 21:55:50.932301	familia10@test.cl	t	Alejandra	Pérez	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000010	APODERADO	40001001-0	\N	\N	\N
94	t	2025-08-22 21:55:50.932301	familia11@test.cl	t	Lorena	López	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000011	APODERADO	40001001-1	\N	\N	\N
95	t	2025-08-22 21:55:50.932301	familia12@test.cl	t	Paola	García	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000012	APODERADO	40001001-2	\N	\N	\N
96	t	2025-08-22 21:55:50.932301	familia13@test.cl	t	Verónica	Martínez	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000013	APODERADO	40001001-3	\N	\N	\N
97	t	2025-08-22 21:55:50.932301	familia14@test.cl	t	Carolina	Sánchez	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000014	APODERADO	40001001-4	\N	\N	\N
98	t	2025-08-22 21:55:50.932301	familia15@test.cl	t	Daniela	Ramos	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000015	APODERADO	40001001-5	\N	\N	\N
99	t	2025-08-22 21:55:50.932301	familia16@test.cl	t	Marcela	Flores	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000016	APODERADO	40001001-6	\N	\N	\N
100	t	2025-08-22 21:55:50.932301	familia17@test.cl	t	Gladys	Contreras	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000017	APODERADO	40001001-7	\N	\N	\N
101	t	2025-08-22 21:55:50.932301	familia18@test.cl	t	Cecilia	Parra	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000018	APODERADO	40001001-8	\N	\N	\N
102	t	2025-08-22 21:55:50.932301	familia19@test.cl	t	Roxana	Aguilar	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000019	APODERADO	40001001-9	\N	\N	\N
103	t	2025-08-22 21:55:50.932301	familia20@test.cl	t	Ingrid	Fuentes	$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq	+56911000020	APODERADO	40001002-0	\N	\N	\N
\.


--
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.usuarios (id, email, email_verified, fecha_registro, first_name, is_active, last_name, password, phone, profile_image, puntaje, rol, updated_at, username) FROM stdin;
1	jorge.gangale@gmail.com	t	2025-08-21 12:09:51.449923	Jorge	t	Gangale	admin123	\N	\N	0	ADMIN	\N	jorge.gangale@gmail.com
\.


--
-- Name: applications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.applications_id_seq', 48, true);


--
-- Name: documents_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.documents_id_seq', 30, true);


--
-- Name: email_events_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.email_events_id_seq', 2, true);


--
-- Name: email_notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.email_notifications_id_seq', 8, true);


--
-- Name: email_verification_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.email_verification_tokens_id_seq', 1, false);


--
-- Name: email_verifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.email_verifications_id_seq', 7, true);


--
-- Name: evaluation_schedules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.evaluation_schedules_id_seq', 1, false);


--
-- Name: evaluations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.evaluations_id_seq', 34, true);


--
-- Name: guardians_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.guardians_id_seq', 31, true);


--
-- Name: interviews_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.interviews_id_seq', 7, true);


--
-- Name: parents_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.parents_id_seq', 98, true);


--
-- Name: progreso_usuario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.progreso_usuario_id_seq', 1, false);


--
-- Name: ranking_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.ranking_id_seq', 1, false);


--
-- Name: students_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.students_id_seq', 51, true);


--
-- Name: supporters_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.supporters_id_seq', 31, true);


--
-- Name: temas_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.temas_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 106, true);


--
-- Name: usuarios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.usuarios_id_seq', 1, true);


--
-- Name: applications applications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_pkey PRIMARY KEY (id);


--
-- Name: documents documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);


--
-- Name: email_events email_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_events
    ADD CONSTRAINT email_events_pkey PRIMARY KEY (id);


--
-- Name: email_notifications email_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_notifications
    ADD CONSTRAINT email_notifications_pkey PRIMARY KEY (id);


--
-- Name: email_notifications email_notifications_response_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_notifications
    ADD CONSTRAINT email_notifications_response_token_key UNIQUE (response_token);


--
-- Name: email_notifications email_notifications_tracking_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_notifications
    ADD CONSTRAINT email_notifications_tracking_token_key UNIQUE (tracking_token);


--
-- Name: email_verification_tokens email_verification_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id);


--
-- Name: email_verifications email_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications
    ADD CONSTRAINT email_verifications_pkey PRIMARY KEY (id);


--
-- Name: evaluation_schedules evaluation_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation_schedules
    ADD CONSTRAINT evaluation_schedules_pkey PRIMARY KEY (id);


--
-- Name: evaluations evaluations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT evaluations_pkey PRIMARY KEY (id);


--
-- Name: guardians guardians_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guardians
    ADD CONSTRAINT guardians_pkey PRIMARY KEY (id);


--
-- Name: interviews interviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interviews
    ADD CONSTRAINT interviews_pkey PRIMARY KEY (id);


--
-- Name: kinder_teachers kinder_teachers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kinder_teachers
    ADD CONSTRAINT kinder_teachers_pkey PRIMARY KEY (id);


--
-- Name: parents parents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT parents_pkey PRIMARY KEY (id);


--
-- Name: problemas problemas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.problemas
    ADD CONSTRAINT problemas_pkey PRIMARY KEY (id);


--
-- Name: professors professors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professors
    ADD CONSTRAINT professors_pkey PRIMARY KEY (id);


--
-- Name: progreso_usuario progreso_usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progreso_usuario
    ADD CONSTRAINT progreso_usuario_pkey PRIMARY KEY (id);


--
-- Name: psychologists psychologists_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.psychologists
    ADD CONSTRAINT psychologists_pkey PRIMARY KEY (id);


--
-- Name: ranking ranking_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ranking
    ADD CONSTRAINT ranking_pkey PRIMARY KEY (id);


--
-- Name: students students_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (id);


--
-- Name: students students_rut_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_rut_key UNIQUE (rut);


--
-- Name: support_staff support_staff_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.support_staff
    ADD CONSTRAINT support_staff_pkey PRIMARY KEY (id);


--
-- Name: supporters supporters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supporters
    ADD CONSTRAINT supporters_pkey PRIMARY KEY (id);


--
-- Name: temas temas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.temas
    ADD CONSTRAINT temas_pkey PRIMARY KEY (id);


--
-- Name: ranking uk25758bs2emn7kip8o039qp3jn; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ranking
    ADD CONSTRAINT uk25758bs2emn7kip8o039qp3jn UNIQUE (usuario_id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: applications uk8yxcoymjxse3o3v2blmj3elya; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT uk8yxcoymjxse3o3v2blmj3elya UNIQUE (guardian_id);


--
-- Name: applications ukbjc0uvubm2oywqk7gpgdqigt1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT ukbjc0uvubm2oywqk7gpgdqigt1 UNIQUE (student_id);


--
-- Name: email_verification_tokens ukewmvysc7e9y6uy7og2c21axa9; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT ukewmvysc7e9y6uy7og2c21axa9 UNIQUE (token);


--
-- Name: applications ukg3exv20ni4ytaxyabji4f087i; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT ukg3exv20ni4ytaxyabji4f087i UNIQUE (father_id);


--
-- Name: applications ukgwerd1mccqjwm1loopov582bp; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT ukgwerd1mccqjwm1loopov582bp UNIQUE (supporter_id);


--
-- Name: usuarios ukkfsp0s1tflm1cwlj8idhqsad0; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT ukkfsp0s1tflm1cwlj8idhqsad0 UNIQUE (email);


--
-- Name: usuarios ukm2dvbwfge291euvmk6vkkocao; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT ukm2dvbwfge291euvmk6vkkocao UNIQUE (username);


--
-- Name: applications uknj2nqjrdbo9g3ywd8mhf934tx; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT uknj2nqjrdbo9g3ywd8mhf934tx UNIQUE (mother_id);


--
-- Name: users ukscuj1snh0iy35s195t3qff5o; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukscuj1snh0iy35s195t3qff5o UNIQUE (rut);


--
-- Name: interviews unique_interviewer_datetime; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interviews
    ADD CONSTRAINT unique_interviewer_datetime UNIQUE (interviewer_user_id, scheduled_date, scheduled_time);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: idx_email_events_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_events_created_at ON public.email_events USING btree (created_at);


--
-- Name: idx_email_events_notification; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_events_notification ON public.email_events USING btree (email_notification_id);


--
-- Name: idx_email_events_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_events_type ON public.email_events USING btree (event_type);


--
-- Name: idx_email_notifications_application; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_notifications_application ON public.email_notifications USING btree (application_id);


--
-- Name: idx_email_notifications_response_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_notifications_response_token ON public.email_notifications USING btree (response_token);


--
-- Name: idx_email_notifications_sent_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_notifications_sent_at ON public.email_notifications USING btree (sent_at);


--
-- Name: idx_email_notifications_tracking_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_notifications_tracking_token ON public.email_notifications USING btree (tracking_token);


--
-- Name: idx_email_notifications_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_notifications_type ON public.email_notifications USING btree (email_type);


--
-- Name: idx_interviews_application_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_application_id ON public.interviews USING btree (application_id);


--
-- Name: idx_interviews_completed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_completed_at ON public.interviews USING btree (completed_at);


--
-- Name: idx_interviews_date_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_date_status ON public.interviews USING btree (scheduled_date, status);


--
-- Name: idx_interviews_follow_up; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_follow_up ON public.interviews USING btree (follow_up_required) WHERE (follow_up_required = true);


--
-- Name: idx_interviews_interviewer_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_interviewer_date ON public.interviews USING btree (interviewer_user_id, scheduled_date);


--
-- Name: idx_interviews_interviewer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_interviewer_id ON public.interviews USING btree (interviewer_user_id);


--
-- Name: idx_interviews_mode; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_mode ON public.interviews USING btree (mode);


--
-- Name: idx_interviews_result; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_result ON public.interviews USING btree (result);


--
-- Name: idx_interviews_scheduled_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_scheduled_date ON public.interviews USING btree (scheduled_date);


--
-- Name: idx_interviews_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_status ON public.interviews USING btree (status);


--
-- Name: idx_interviews_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interviews_type ON public.interviews USING btree (type);


--
-- Name: email_notifications trigger_email_notifications_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trigger_email_notifications_updated_at BEFORE UPDATE ON public.email_notifications FOR EACH ROW EXECUTE FUNCTION public.update_email_notifications_updated_at();


--
-- Name: interviews trigger_update_interviews_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trigger_update_interviews_updated_at BEFORE UPDATE ON public.interviews FOR EACH ROW EXECUTE FUNCTION public.update_interviews_updated_at();


--
-- Name: interviews trigger_validate_completed_interview; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trigger_validate_completed_interview BEFORE INSERT OR UPDATE ON public.interviews FOR EACH ROW EXECUTE FUNCTION public.validate_completed_interview();


--
-- Name: email_events email_events_email_notification_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_events
    ADD CONSTRAINT email_events_email_notification_id_fkey FOREIGN KEY (email_notification_id) REFERENCES public.email_notifications(id) ON DELETE CASCADE;


--
-- Name: email_notifications email_notifications_application_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_notifications
    ADD CONSTRAINT email_notifications_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.applications(id) ON DELETE CASCADE;


--
-- Name: email_notifications email_notifications_interview_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_notifications
    ADD CONSTRAINT email_notifications_interview_id_fkey FOREIGN KEY (interview_id) REFERENCES public.interviews(id) ON DELETE SET NULL;


--
-- Name: progreso_usuario fk31f8nnpf2w40at3hwrljvcurw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progreso_usuario
    ADD CONSTRAINT fk31f8nnpf2w40at3hwrljvcurw FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: psychologist_grades fk4crcq1yuj1095h0plv0jx254q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.psychologist_grades
    ADD CONSTRAINT fk4crcq1yuj1095h0plv0jx254q FOREIGN KEY (psychologist_id) REFERENCES public.psychologists(id);


--
-- Name: evaluations fk4dmy35k49uvtqh1r5mu6hu9t7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT fk4dmy35k49uvtqh1r5mu6hu9t7 FOREIGN KEY (application_id) REFERENCES public.applications(id);


--
-- Name: professor_qualifications fk5b7qr0spgjspxc0ht26i67omm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professor_qualifications
    ADD CONSTRAINT fk5b7qr0spgjspxc0ht26i67omm FOREIGN KEY (professor_id) REFERENCES public.professors(id);


--
-- Name: problemas fk734g8qk965lgfaawmemsx7quq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.problemas
    ADD CONSTRAINT fk734g8qk965lgfaawmemsx7quq FOREIGN KEY (tema_id) REFERENCES public.temas(id);


--
-- Name: documents fk8umh06sslm8f0rbfasqk6yy0f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk8umh06sslm8f0rbfasqk6yy0f FOREIGN KEY (application_id) REFERENCES public.applications(id);


--
-- Name: evaluation_schedules fk929j8xym0wlneuonx5ey3twxf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation_schedules
    ADD CONSTRAINT fk929j8xym0wlneuonx5ey3twxf FOREIGN KEY (confirmed_by_user_id) REFERENCES public.users(id);


--
-- Name: kinder_teacher_qualifications fk9fa8xuxbcpjkxxql1r450c6xn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kinder_teacher_qualifications
    ADD CONSTRAINT fk9fa8xuxbcpjkxxql1r450c6xn FOREIGN KEY (teacher_id) REFERENCES public.kinder_teachers(id);


--
-- Name: interviews fk_interview_application; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interviews
    ADD CONSTRAINT fk_interview_application FOREIGN KEY (application_id) REFERENCES public.applications(id) ON DELETE CASCADE;


--
-- Name: interviews fk_interview_interviewer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interviews
    ADD CONSTRAINT fk_interview_interviewer FOREIGN KEY (interviewer_user_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: applications fkbxjuiec753shgoyw6x0l8opn8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT fkbxjuiec753shgoyw6x0l8opn8 FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: psychologist_specialized_areas fkclqeyjswsjmfgow2qvcaybq7r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.psychologist_specialized_areas
    ADD CONSTRAINT fkclqeyjswsjmfgow2qvcaybq7r FOREIGN KEY (psychologist_id) REFERENCES public.psychologists(id);


--
-- Name: evaluations fkct8v0spukoo68fa2p9suju4nc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT fkct8v0spukoo68fa2p9suju4nc FOREIGN KEY (schedule_id) REFERENCES public.evaluation_schedules(id);


--
-- Name: professor_grades fkcyeelpt968uweivesr2923vgj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professor_grades
    ADD CONSTRAINT fkcyeelpt968uweivesr2923vgj FOREIGN KEY (professor_id) REFERENCES public.professors(id);


--
-- Name: ranking fkdk59vfgtvw0qr12a4y9fh5xwn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ranking
    ADD CONSTRAINT fkdk59vfgtvw0qr12a4y9fh5xwn FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: psychologists fke9xl394g2y647yscq6tdlut4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.psychologists
    ADD CONSTRAINT fke9xl394g2y647yscq6tdlut4 FOREIGN KEY (id) REFERENCES public.usuarios(id);


--
-- Name: kinder_teachers fkex1e8gqykdk8vljxhhd504n8f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kinder_teachers
    ADD CONSTRAINT fkex1e8gqykdk8vljxhhd504n8f FOREIGN KEY (id) REFERENCES public.usuarios(id);


--
-- Name: evaluation_schedules fkf905brl97n1s0uyjvmoqsry4g; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation_schedules
    ADD CONSTRAINT fkf905brl97n1s0uyjvmoqsry4g FOREIGN KEY (evaluator_id) REFERENCES public.users(id);


--
-- Name: professors fkfvoolphnt6fjvmat24p26u57m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professors
    ADD CONSTRAINT fkfvoolphnt6fjvmat24p26u57m FOREIGN KEY (id) REFERENCES public.usuarios(id);


--
-- Name: applications fkhobxf5y55guo775hl1agtlvmr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT fkhobxf5y55guo775hl1agtlvmr FOREIGN KEY (supporter_id) REFERENCES public.supporters(id);


--
-- Name: support_staff fki4tt3e4g95rwp2p81h8286r26; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.support_staff
    ADD CONSTRAINT fki4tt3e4g95rwp2p81h8286r26 FOREIGN KEY (id) REFERENCES public.usuarios(id);


--
-- Name: support_staff_responsibilities fkjqrnfl5y2uo5pjbi1t2klmt2s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.support_staff_responsibilities
    ADD CONSTRAINT fkjqrnfl5y2uo5pjbi1t2klmt2s FOREIGN KEY (staff_id) REFERENCES public.support_staff(id);


--
-- Name: progreso_usuario fkk8nu4vuk3au5efqu76gtapa2j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progreso_usuario
    ADD CONSTRAINT fkk8nu4vuk3au5efqu76gtapa2j FOREIGN KEY (problema_id) REFERENCES public.problemas(id);


--
-- Name: applications fkm3ipmdilwyvhhaq4jaim7pvun; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT fkm3ipmdilwyvhhaq4jaim7pvun FOREIGN KEY (guardian_id) REFERENCES public.guardians(id);


--
-- Name: applications fkm6w0opoixba8hisj51yapw2yw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT fkm6w0opoixba8hisj51yapw2yw FOREIGN KEY (mother_id) REFERENCES public.parents(id);


--
-- Name: kinder_teacher_specializations fkmd9w3lglly5vh5iyd8cdgjnvb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kinder_teacher_specializations
    ADD CONSTRAINT fkmd9w3lglly5vh5iyd8cdgjnvb FOREIGN KEY (teacher_id) REFERENCES public.kinder_teachers(id);


--
-- Name: applications fknh2osxuurfe1dtopyyxi9j4k0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT fknh2osxuurfe1dtopyyxi9j4k0 FOREIGN KEY (applicant_user_id) REFERENCES public.users(id);


--
-- Name: evaluations fkqihdmjba0yaamhjp8gr00c27m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT fkqihdmjba0yaamhjp8gr00c27m FOREIGN KEY (evaluator_id) REFERENCES public.users(id);


--
-- Name: professor_subjects fksjxqv931ljk4uuof4ob6vopga; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professor_subjects
    ADD CONSTRAINT fksjxqv931ljk4uuof4ob6vopga FOREIGN KEY (professor_id) REFERENCES public.professors(id);


--
-- Name: evaluation_schedules fksw5iqjypi6m4tf2xgvo9xnvm7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation_schedules
    ADD CONSTRAINT fksw5iqjypi6m4tf2xgvo9xnvm7 FOREIGN KEY (application_id) REFERENCES public.applications(id);


--
-- Name: applications fktr3t9onllcgbtsocwyqf7c1ay; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT fktr3t9onllcgbtsocwyqf7c1ay FOREIGN KEY (father_id) REFERENCES public.parents(id);


--
-- PostgreSQL database dump complete
--

\unrestrict MmgIiHZMzFGyZDvYnsdsPyF4mjxPa91qAviLmcGGBbPRpgDTh4zKJljJDdydTtS

