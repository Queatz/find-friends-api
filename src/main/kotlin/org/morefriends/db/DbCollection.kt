package org.morefriends.db

import org.morefriends.models.Attend as AttendModel
import org.morefriends.models.Code as CodeModel
import org.morefriends.models.Feedback as FeedbackModel
import org.morefriends.models.Idea as IdeaModel
import org.morefriends.models.Meet as MeetModel
import org.morefriends.models.Place as PlaceModel
import org.morefriends.models.Problem as ProblemModel
import org.morefriends.models.Quiz as QuizModel
import org.morefriends.models.Vote as VoteModel
import org.morefriends.models.Confirm as ConfirmModel
import org.morefriends.models.Model
import kotlin.reflect.KClass

enum class DbCollection(val model: KClass<out Model>) {
    Attend(AttendModel::class),
    Code(CodeModel::class),
    Feedback(FeedbackModel::class),
    Idea(IdeaModel::class),
    Meet(MeetModel::class),
    Place(PlaceModel::class),
    Problem(ProblemModel::class),
    Quiz(QuizModel::class),
    Vote(VoteModel::class),
    Confirm(ConfirmModel::class),
}
