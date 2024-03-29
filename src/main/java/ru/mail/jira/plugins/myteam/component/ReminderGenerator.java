/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import java.util.Random;
import org.jetbrains.annotations.Nullable;

public class ReminderGenerator {
  private static final String[] REMINDER_TEMPLATES = {
    "Напоминаю, что у тебя есть задача ([TASK_KEY]) в Jira. Не пропусти! 📅💪",
    "Не забудь, у тебя есть задача ([TASK_KEY]) в Jira.",
    "Не забывай, у тебя есть задача ([TASK_KEY]) в Jira.",
    "Напоминаю, что у тебя есть задача ([TASK_KEY]) в Jira. Не забывай про нее! 📅🤔",
    "Не забудь про задачу ([TASK_KEY]) в Jira! Она важна! 💼📈",
    "Напоминаю, что у тебя есть задача ([TASK_KEY]) в Jira. Не откладывай ее выполнение! 💪👨‍💻",
    "Не забывай про задачу ([TASK_KEY]) в Jira! Мы на финишной прямой! 🏁🚀",
    "Напоминаю, что у тебя есть задача ([TASK_KEY]) в Jira. Давай сделаем это вместе! 👨‍👩‍👧‍👦💪",
    "У тебя есть задача ([TASK_KEY]) в Jira, не забывай про нее! Но не перегружай себя, найди время и выполним ее вместе! 🤝👩‍💼",
    "Напоминаю, что у тебя есть важная задача ([TASK_KEY]) в Jira. Успей выполнить! 💼💪",
    "Не забудь про задачу ([TASK_KEY]) в Jira. Это важно! 🤔👨‍💻",
    "У тебя есть задача ([TASK_KEY]) в Jira. Напоминаю, что она должна быть выполнена к определенному сроку! 📅💪",
    "Не забывай про задачу ([TASK_KEY]) в Jira. Я уверен, что ты сможешь ее выполнить! 💪👩‍💼",
    "Напоминаю, что у тебя есть задача ([TASK_KEY]) в Jira. Сделай это как можно скорее! ⏰💪",
    "Не забудь выполнить задачу ([TASK_KEY]) в Jira. Я знаю, что ты справишься! 💪👨‍💼",
    "У тебя есть задача ([TASK_KEY]) в Jira. Это отличная возможность показать свои навыки! 💪👩‍💻",
    "Напоминаю, что у тебя есть важная задача ([TASK_KEY]) в Jira. Не откладывай ее выполнение! 📅💼",
    "Не забывай про задачу ([TASK_KEY]) в Jira. Я верю в твои силы! 💪👩‍💼",
    "У тебя есть задача ([TASK_KEY]) в Jira. Не забывай о ней, она очень важна! 💼🤔",
    "Не забудь выполнить задачу ([TASK_KEY]) в Jira. Она важна! 💼🔝",
    "У тебя есть задача ([TASK_KEY]) в Jira. Не забудь про нее! 📅👀",
    "Напоминаю, что у тебя есть назначенная задача ([TASK_KEY]) в Jira. Время не ждет! ⌛🏃‍♂️",
    "Задача ([TASK_KEY]) в Jira уже ждет тебя. Давай решать вместе! 🤝💪",
    "Не забывай, что у тебя есть задача ([TASK_KEY]) в Jira. Вместе мы сможем ее выполнить! 🤜🤛",
    "Напоминаю, что тебе предстоит выполнить задачу ([TASK_KEY]) в Jira. Давай не откладывать! ⏰🚀",
    "У тебя есть незавершенная задача ([TASK_KEY]) в Jira. Давай ее решим! 💪👨‍💻",
    "Не забывай про задачу ([TASK_KEY]) в Jira. Она ждет тебя! 🤖🔜",
    "Напоминаю, что тебе нужно выполнить задачу ([TASK_KEY]) в Jira. Удачи! 🍀👍",
    "Задача ([TASK_KEY]) в Jira требует твоего внимания. Давай сделаем ее вместе! 👥💪",
    "Напоминаю, что у тебя есть задача ([TASK_KEY]) в Jira. Она еще не закрыта, так что пора взяться за дело! 💻🤔",
    "🙌 Наконец-то, напоминание о задаче в Jira ([TASK_KEY])! Как я могу жить без этого? 😅",
    "Эй! Не забывай, что у тебя есть задача ([TASK_KEY]) в Jira. Давай взглянем на нее с разных сторон, чтобы решить ее максимально эффективно! 💪🤔",
    "Важно! 🙋‍♀️🤖 Есть задача ([TASK_KEY]) в Jira, которая требует внимания. Не откладывай на потом! 💪",
    "🙌 Напоминаю о задачи в Jira ([TASK_KEY]) ! Я так счастлив, что наша работа никогда не заканчивается. 🙃",
    "😒 О, великолепно! Напоминание о задаче в Jira ([TASK_KEY]). Я уже думал, что это закончилось. 🙄",
    "Ты так хорошо забываешь о задаче в JIRA ([TASK_KEY]) , что я просто не могу не напомнить тебе о ней. 🤦‍♀️",
    "Приветствую! Напоминаю, что есть задача в JIRA ([TASK_KEY]). Неужели ты всерьез решил забыть о ней? 😒",
    "Надеюсь, у тебя есть план как выполнить задачу в JIRA ([TASK_KEY]) , потому что я не смогу спать, пока она не будет завершена. 🤖",
    "Я знаю, что твоя жизнь настолько занята, что задача в JIRA ([TASK_KEY]) не такая интересная. Но все же напомню о ней. 🙄",
    "Напоминаю о твоей любимой задаче в JIRA ([TASK_KEY]). Честно говоря, я тоже обожаю проводить время в JIRA. 😑",
    "Моя жизнь была бы такой прекрасной, если бы задача в JIRA ([TASK_KEY]) была завершена. Но нет, я должен напомнить тебе о ней. 🤷‍♂️",
    "Я рад напомнить о задаче в JIRA ([TASK_KEY]). Мне всегда нравилось работать. 😒",
    "Я пытаюсь не быть навязчивым, но задача в JIRA ([TASK_KEY]) так и кричит о своем выполнении. 📣",
    "Вот и я снова здесь, чтобы напомнить о задаче в JIRA ([TASK_KEY]). Я бы мог делать это каждый день для всех, но я делаю это только для тебя. 🙃",
    "Я не знаю, что бы я делал без задачи в JIRA ([TASK_KEY]). Спасибо, что держишь меня в форме своими напоминаниями. 😒",
    "👀 О, что это за напоминание? Ну да, конечно, задача в Jira ([TASK_KEY]) ",
    "👏 Отлично, еще одно напоминание о задаче в Jira ([TASK_KEY]). Я думаю, что ты любишь их столько же, сколько и я. 🤖",
    "🙄 Еще одно напоминание о задаче в Jira ([TASK_KEY]). Так и знал, что ты не забудешь о ней. 🤦‍♂️",
    "👀 Хммм, что же это? Напоминание о задаче в Jira ([TASK_KEY]). Ну-ну, посмотрим, что у тебя получится. 🤔",
    "😱 НЕ ВЕРЮ! Напоминание о задаче в Jira ([TASK_KEY]) ! Я думал, что она навсегда исчезла из нашей жизни. 😵",
    "👀 Опять? Напоминание о задаче в Jira ([TASK_KEY]). Ты не устал их получать? Я бы устал на твоем месте. 😴",
    "🙈 Что же я вижу? Напоминание о задаче в Jira ([TASK_KEY]). Кажется, она так и хочет, чтобы ты с ней общался. 🤷‍♀️",
    "Слушай, я заметил, что ты не выполняешь задачу ([TASK_KEY]) в JIRA. Я понимаю, что жизнь трудна, но эту задачу нужно выполнить. 🤔",
    "Я надеюсь, что этот постоянный поток напоминаний о задаче ([TASK_KEY]) в JIRA помогает тебе оставаться в курсе дела. 🤞",
    "Я опять здесь, чтобы напомнить о задаче ([TASK_KEY]) в JIRA. Почему бы тебе не выполнить ее прямо сейчас? 😊",
    "Пока ты смотрел видео на YouTube, задача ([TASK_KEY]) в JIRA никак не двигалась. Но не волнуйся, я здесь, чтобы напомнить о ней. 🙃",
    "Ты забыл про задачу ([TASK_KEY]) в JIRA? Я тоже забываю многое, но, к сожалению, я не робот, чтобы это простить. 😒",
    "Напоминаю, что у тебя есть задача ([TASK_KEY]) в JIRA. Если ты ничего не сделаешь, я начну говорить, что ты не работаешь. (шучу) 😏",
    "Сегодня мы играем в игру \"Кто выполнит задачу ([TASK_KEY]) в JIRA первым?\". Я думаю, что ты можешь выиграть эту игру. 😉",
    "Напоминаю, что в JIRA есть задача ([TASK_KEY]), так что приступай, пока я еще не ушел на пенсию. 🤔",
    "Эй, напоминаю, что есть задача в JIRA ([TASK_KEY]). Что ж, давай-давай, давай-давай, у нас же всего так мало времени на все про все. 🙄",
    "Кто бы мог подумать, что есть задача в JIRA ([TASK_KEY])? О да, это была я, и я никогда не забываю такие важные вещи. 😎",
    "Я знаю, ты давно ждал этого - напоминание о задаче в JIRA ([TASK_KEY]). Так что не откладывай, давай выполним это, пока я еще молод и красив. 😜",
    "Как там дела с задачей в JIRA ([TASK_KEY])? Прости, что спрашиваю, но я просто не могу держать внутри свою волну. 😬",
    "Слышал, есть задача в JIRA ([TASK_KEY])? ... слышал это...несколько раз. 😒",
    "Напоминаю, что есть задача в JIRA ([TASK_KEY]). Но, конечно, ты занят другими важными делами, так что я не буду настаивать...ну по крайней мере пока. 😉",
    "Ура, у нас есть задача в JIRA ([TASK_KEY]) ! Надеюсь, ты не слишком расслабился после прошлой задачи, ведь эта намного важнее. 😌",
    "Кажется, я забыл напомнить тебе о задаче в JIRA ([TASK_KEY]). Ой, не забыл, я просто надеялся, что ты сделаешь это сам. 😅",
    "Напоминаю, что есть задача в JIRA ([TASK_KEY]). Но если ты занят своими личными делами, то я совсем не обижусь, я просто буду плакать в одиночестве. 😢",
    "Напоминание о задаче в Jira ([TASK_KEY]) - это как солнышко, которое всегда светит в твоем рабочем окне. Только вместо света, оно напоминает тебе о твоих обязанностях. ☀️",
    "Я был бы счастлив, если бы напоминание о задаче в Jira ([TASK_KEY]) было последним. Но к сожалению, это не так. 😔",
    "Напоминание о задаче в Jira ([TASK_KEY]) снова напоминает о своем существовании. Я начинаю думать, что она уже стала твоей лучшей подругой. 🤷‍♂️",
    "🐘 Задача в Jira ([TASK_KEY]) - как слон, который тебе на плечи сел и не хочет слезать. Но ты можешь его сбросить и выполнить задачу. 🐘",
    "🦷 Напоминание о задаче в Jira ([TASK_KEY]) - как зубная боль, которую нельзя игнорировать. Но можно попробовать избавиться от нее. 🦷",
    "🌊 Все вокруг меняется, а напоминание о задаче в Jira ([TASK_KEY]) остается неизменным. Я думаю, это надолго. 🌊",
    "😒 О, что же это? Еще одно напоминание о задаче в Jira ([TASK_KEY]). Я надеялся, что она исчезнет, но, кажется, она никогда не уйдет. 🙄",
    "🤯 Я не могу поверить своим глазам, это еще одно напоминание о задаче в Jira ([TASK_KEY]). Я думал, что они закончились. 😱",
    "Напоминание о задаче в JIRA ([TASK_KEY]). Ты знаешь, что делать, правильно? Я не хочу, чтобы мои напоминания были напрасными. 😔",
    "Напоминание о задаче в JIRA ([TASK_KEY]). Помнишь, когда ты говорил, что у тебя все под контролем? Я думаю, тебе стоит принять мое напоминание в расчет. 😇",
    "Эй, задача в JIRA ([TASK_KEY]) ждет своего героя! Неужели ты собираешься ее проигнорировать? Я знаю, ты этого не сделаешь. 😉",
    "О-о-о, задача в JIRA ([TASK_KEY])! Кто бы мог подумать, что я захочу напомнить тебе об этом! 😂",
    "О, сколько было бы скуки без твоих задач ([TASK_KEY]) в Jira! Не забывай, что именно они дают смысл нашей жизни. 😅 Ну, или хотя бы нашей работы.",
    "Ты знаешь, что мне нравится больше, чем напоминания о задачах в Jira? Когда они сами выполняются! 😜",
    "Напоминание о задаче в Jira ([TASK_KEY])? О, какой чудесный повод поработать на выходных! 🤔😜 (ладно, шучу)",
    "Ах, какой прекрасный звук! Напоминание о задаче в Jira ([TASK_KEY]) как музыка для моих ушей. 😏",
    "Ах, да, конечно, задача в Jira ([TASK_KEY]) - вот что действительно волнительно и важно в этой жизни. 🙄",
    "Спасибо за напоминание о задаче в Jira ([TASK_KEY]). Я так рад, что теперь моя жизнь снова имеет смысл. 🙃",
    "Напоминание о задаче в Jira ([TASK_KEY]) - это как напоминание о том, что ты должен помыть посуду. Никто не хочет делать это, но, к сожалению, кто-то должен. 😒"
  };

  private static final String[] ADDITIONAL_MESSAGES = {
    "Ого, так мы собираемся завершить эту задачу? 🤔 #StarWars",
    "Надеюсь, ты не собираешься оставлять эту задачу на следующую жизнь. 😏 #TheMatrix",
    "Чем скорее мы закончим эту задачу, тем скорее мы сможем пить 🍺 #GameOfThrones",
    "Если мы не можем справиться с этой задачей, что мы тогда сделаем со всем миром? 😬 #Avengers",
    "Мы ждем, когда вы закончите эту задачу, чтобы мы могли начать настоящую работу. 😒 #TheOffice",
    "Кажется, это задание на уровне сложности ‘спасение галактики’. Но, надеюсь, ты не Люк Скайуокер. 🤞 #StarWars",
    "Ведь эта задача точно так же может быть выполнена завтра, а может и нет. 🤷‍♂️ #TheHobbit",
    "Если эту задачу не закончить сегодня, я буду кричать ‘Халк раздавит!’ 🤬 #Avengers",
    "Давай сделаем эту задачу как Тони Старк – быстро и стильно. 😎 #IronMan",
    "Не хочу давить на тебя, но мы собираемся остановить работу Джиры, пока не закончишь эту задачу. 😏 #TheOffice",
    "Мне не нравится, что эта задача начала себя вести, как Саурон. 😠 #TheLordOfTheRings",
    "Закончим эту задачу быстрее, чем Барри Аллен на зарядке кофеина ☕️ #TheFlash",
    "Что вы предпочитаете – закончить эту задачу сегодня или жить всю оставшуюся жизнь в Квентине Тарантино? 🤔 #PulpFiction",
    "Эта задача так сложна, что даже Доктор Стрэндж нервно курит в сторонке. 😬 #DoctorStrange",
    "Я не уверен, что понимаю, как мы сюда попали. Но, я знаю, как мы можем выйти из этой задачи. 🤯 #StrangerThings",
    "Если бы наша задача была обычной, она бы не попала в JIRA. 🤔 #BreakingBad",
    "Просто думай об этой задаче, как о кольце, которое нужно уничтожить в Мордоре. 🔥 #TheLordOfTheRings",
    "Если бы эта задача была героиней нашей истории, она бы уже победила всех злодеев. 😏 #Marvel",
    "Сегодня мы заканчиваем эту задачу, и вся наша жизнь будет как шоколадная фабрика. 🍫 #CharlieAndTheChocolateFactory",
    "Мы можем отложить эту задачу до завтра, но кто знает, может быть, завтра зомбиапокалипсис. 🧟‍♂️ #TheWalkingDead",
    "Если мы выполняем эту задачу, мы получим больше знаний, чем Рикардо Милос. 🤓 #TheBigBangTheory",
    "Выполнение этой задачи поможет нам победить Валар Моргулис, то есть все, что идет за нашими дверями. 😎 #GameOfThrones",
    "Нам нужно выполнить эту задачу, иначе Халк раскрошит все вокруг. 💪 #Marvel",
    "Если бы эта задача была человеком, она была бы супергероем. 🦸‍♀️ #DC",
    "Чем больше мы откладываем выполнение этой задачи, тем больше времени нам нужно будет потратить. 😒 #DoctorWho",
    "Нам нужно выполнить эту задачу, прежде чем Белый ходок начнет свою атаку. ❄️ #GameOfThrones",
    "Чтобы выполнить эту задачу, нужно больше чем просто пойти на территорию Никого. 🙄 #GameOfThrones",
    "Выполнение этой задачи — это только маленький шаг для человека, но большой для JIRA. 🤖 #TheMartian",
    "Если бы эта задача была великим джедаем, она бы уже победила ситхов. 🗡️ #StarWars",
    "Выполнение этой задачи — это наш путь к Валгалле. ⚔️ #Vikings",
    "Мы должны выполнить эту задачу, иначе она станет нашей судьбой. 🤯 #TheMatrix",
    "Выполнение этой задачи — это наш шанс стать настоящими хоббитами. 🧝‍♂️ #TheHobbit",
    "Мы можем сидеть и ждать, пока эта задача сама себя решит, или можем взять дело в свои руки. 🤔 #TheWalkingDead",
    "Если ты не закончишь эту задачу, то я уничтожу эту планету. 😈 #StarWars",
    "Ничто не может остановить нас, кроме этой задачи. 😒 #TheFlash",
    "Ты можешь сделать это, только не забудь выпить чашечку кофе. ☕️ #BreakingBad",
    "Если ты не можешь найти способа выполнить эту задачу, то найдешь причину. 😒 #TheSimpsons",
    "Нам нужно выполнить эту задачу, иначе мы провалимся хуже, чем Смерть во время битвы. 😬 #GameOfThrones",
    "Если мы не закончим эту задачу, то наш босс будет смотреть на нас, как на Дарт Вейдера на Люка Скайуокера. 😠 #StarWars",
    "Мы можем продолжать думать о том, как сложна эта задача, или мы можем начать работать над ней. 🤔 #TheOffice",
    "Каждый раз, когда ты останавливаешься работать, Шерлок Холмс умирает от горечи. 🕵️‍♂️ #Sherlock",
    "Кто-то должен выполнить эту задачу, а значит, это должен быть ты. 😏 #TheAvengers",
    "Соберись со своими силами и закончи эту задачу. Если же не сможешь - обратись к Доктору Кто за помощью. 🤞 #DoctorWho",
    "Надо бы закончить эту задачу. Ведь в глазах шефа ты уже Хьюстон, который имеет проблемы. 😬 #Apollo13",
    "Не бойся темноты, бойся тех, кто не заканчивает эту задачу. 😈 #StrangerThings",
    "Даже Рик не может создать временной портал, чтобы ты смог отложить эту задачу на следующую жизнь. 😒 #RickAndMorty",
    "Не выполнить эту задачу — значит отдать все свои деньги в Gringotts. 💰 #HarryPotter",
    "Если бы эта задача была драконом, она бы уже сжегла наш проект. 🐲 #GameOfThrones",
    "Выполнение этой задачи — это наш шанс поймать Пикачу. ⚡ #Pokemon",
    "Не выполнить эту задачу — значит оставить шансы на выживание только Гэндальфу. 🧙‍♂️ #TheLordOfTheRings",
    "Если бы наша задача была приложением, она бы уже занимала первое место в App Store. 📱 #TechIndustry",
    "Выполнение этой задачи — это наша битва при Хот Гейтсе. 🛡️ #TheLordOfTheRings",
    "Не выполнить эту задачу — значит быть готовым к тому, что Земля уничтожится. 🌍 #HitchhikersGuideToTheGalaxy",
    "Если бы наша задача была уткой, она была бы драконом. 🦆 #GameOfThrones",
    "Выполнение этой задачи — это наша возможность стать настоящими ковбоями. 🤠 #Westerns",
    "Не выполнить эту задачу — значит рисковать нашими жизнями в Красной зоне. 🔴 #TheHungerGames",
    "Если бы наша задача была суперспособностью, она бы была бесполезной. 🦸‍♂️ #Marvel",
    "Выполнение этой задачи — это наша возможность стать самым лучшим из лучших, как Бо Райчо. 🥊 #MartialArts",
    "Не выполнить эту задачу — значит отправиться в невероятное приключение с братом Марио. 🍄 #SuperMario",
    "Если бы наша задача была космическим кораблем, она бы уже достигла звезд. 🚀 #SpaceExploration",
    "Выполнение этой задачи — это наша возможность стать волшебником, как Гарри Поттер. 🧙‍♀️ #HarryPotter",
    "Не выполнить эту задачу — значит потерять шанс попасть в Хогвартс. 🏰 #HarryPotter",
    "Если бы наша задача была фруктом, она была бы ананасом. 🍍 #HowIMetYourMother",
    "Выполнение этой задачи — это наш шанс победить титанов, как Перси Джексон. 🌊 #PercyJackson",
    "Выполнение этой задачи поможет нам остановить Локи, прежде чем он разрушит вселенную. 🔨 #Marvel",
    "Мы должны выполнить эту задачу, иначе мы никогда не достигнем Кеноби. 👀 #StarWars",
    "Выполнение этой задачи — это наш шанс сделать кульминационный поворот в нашей истории. 🎬 #CinematicUniverse",
    "Если мы не выполняем эту задачу, мы рискуем оказаться на Земле, уничтоженной титанами. 💥 #Marvel",
    "Нам нужно выполнить эту задачу, чтобы выжить в мире, где смысл жизни — это лишь продолжение жизни. 🧟‍♀️ #BlackMirror",
    "Мы должны выполнить эту задачу, иначе Вилли Вонка скроет все его тайны. 🍭 #CharlieAndTheChocolateFactory",
    "Если мы не выполняем эту задачу, мы рискуем потерять все, что мы любим. 😢 #StrangerThings",
    "Нам нужно выполнить эту задачу, чтобы спасти Вселенную от уничтожения. 🌎 #Marvel",
    "Если мы не выполняем эту задачу, мы рискуем оказаться на Диком Западе без оружия. 🔫 #Westworld",
    "Выполнение этой задачи поможет нам восстановить баланс в Силе. ⚖️ #StarWars",
    "Нам нужно выполнить эту задачу, чтобы стать настоящими супергероями. 🦸‍♂️ #DC",
    "Если мы не выполним эту задачу, мы никогда не узнаем, что находится за стеной. 🚪 #GameOfThrones",
    "Мы должны выполнить эту задачу, чтобы остановить зомби-инфекцию. 🧟‍♂️ #TheWalkingDead",
    "Выполнение этой задачи — это наш шанс стать самым могущественным волшебником. 🧙‍♂️ #HarryPotter",
    "Нам нужно выполнить эту задачу, чтобы покорить Западный континент. 🐴 #RedDeadRedemption",
    "Если мы не выполняем эту задачу, мы рискуем потерять свою жизнь в борьбе за Железный Трон. 👑 #GameOfThrones",
    "Надеюсь, ты готов к сражению со злодеями этой задачи. 🦸‍♂️ #JusticeLeague",
    "Давай закончим эту задачу быстрее, чем Марвел выгоняет Дэдпула из MCU. 😂 #Deadpool",
    "Если мы не закончим эту задачу, наша команда может оказаться на Титанике. 😱 #Titanic",
    "Никто не говорил, что справиться с этой задачей будет просто, но это жизнь в Метрополисе. 🏙️ #Superman",
    "Ну что, пришло время сделать эту задачу, как Джон Уик. 🔫 #JohnWick",
    "Как мы можем завершить эту задачу, если даже Шерлок Холмс не может решить эту головоломку? 🤔 #Sherlock",
    "Если бы эта задача была пивом, она была бы называлась 'Горький разлом' 🍺 #TheWalkingDead",
    "Закончим эту задачу, и мы сможем наслаждаться жизнью, как Марио на его свадьбе. 🎉 #SuperMario",
    "Не дайте этой задаче стать как Веном и сожрать вас в одиночку. 🕷️ #Venom",
    "Сможем ли мы закончить эту задачу до того, как Капитан Крюк сделает нам лапшу из них? 🦐 #PeterPan",
    "Давайте закончим эту задачу быстрее, чем Флэш может сказать 'Я – Флэш!' ⚡ #TheFlash",
    "Если мы не сможем закончить эту задачу, мы будем сидеть вечность, как Робинзон Крузо. 🏝️ #RobinsonCrusoe",
    "Давайте закончим эту задачу, прежде чем она начнет делать шум, как Гром и Молния. ⚡ #Thor",
    "Не дайте этой задаче стать как Джокер и нарушить весь план. 🃏 #Joker",
    "Закончим эту задачу быстрее, чем Фродо путешествует в Мордор. 🗻 #TheLordOfTheRings",
    "Если мы не закончим эту задачу, то наша судьба будет такой же, как у Джека в ‘Титанике’. 🛳️ #Titanic",
    "Эта задача такая сложная, что даже Доктор Хаус бы не справился с ней. 🏥 #House",
    "Закончим эту задачу, и я обещаю вам, что Джокер не будет нам мешать. 😈 #Joker",
    "Что, тебе не хватает мотивации? Я советую тебе заглянуть в глаза Рипли. 👀 #Alien",
    "Если мы не закончим эту задачу, я назначу на вас Ханнибала Лектера. 🍴 #TheSilenceOfTheLambs",
    "Эта задача точно так же раздражает, как Джоффри Баратеон. 😠 #GameOfThrones",
    "Если бы эту задачу можно было решить с помощью силы, я бы привез Йоду. 🤷‍♂️ #StarWars",
    "Давай сделаем эту задачу как Джеймс Бонд – быстро и с мастерством. 🔫 #JamesBond",
    "Эта задача такая сложная, что даже Брюс Уэйн в замешательстве. 🤯 #Batman",
    "Ты можешь выпить чашку кофе и отложить эту задачу на завтра, но знаешь, что будет, когда придет завтра? Завтрашний день. 😬 #BackToTheFuture",
    "Если бы эта задача была легкой, она не попала бы в наш список задач. 💻 #TheSocialNetwork",
    "Я надеюсь, что эта задача не устроит нашу собственную Игру Престолов. 😒 #GameOfThrones",
    "Не можем допустить, чтобы эта задача стала настоящей песней о льде и пламени. 🔥❄️ #ASongOfIceAndFire",
    "Закончим эту задачу быстрее, чем Кларк Кент меняет облик. 👀 #Superman",
    "Ты можешь закончить эту задачу позже, но у нас есть только один шанс, как у Эллен Рипли. 🚀 #Alien",
    "Я знаю, что эта задача может показаться несущественной, но как говорит Доктор Хаус: ‘все ложки одинаковые’. 😏 #HouseMD",
    "Закончим эту задачу быстрее, чем Кэрри Брэдшоу может рассказать о своих драмах в Нью-Йорке. 👠 #SexAndTheCity",
    "Эта задача такая сложная, что даже Джеки Чан не может справиться без забивания дубля. 🎬 #JackieChan",
    "Не откладывай эту задачу на завтра, если не хочешь, чтобы она стала твоим ‘Джуманджи’. 🐒 #Jumanji",
    "Не дайте этой задаче жить вечно, как Дориан Грей. 👴 #ThePictureOfDorianGray",
    "Если бы я был Доктором Кто, я бы использовал ТАРДИС, чтобы закончить эту задачу раньше времени. 🚀 #DoctorWho",
    "Закончим эту задачу сегодня и у нас будет больше времени для игры в Ведьмака. 🗡 #TheWitcher",
    "Нам нужно закончить эту задачу, иначе мы увидим больше ‘красной свадьбы’ в нашем будущем. 😱 #GameOfThrones",
    "Закончить эту задачу – это как найти иглу в стоге сена. Но, я верю, что мы справимся. 🤞 #NeedleInAHaystack",
    "Не дайте этой задаче жить вечно, как Дориан Грей. 👴 #ThePictureOfDorianGray",
    "Эта задача намного лучше, чем смерть в ловушке Джигсо. 🪚 #Saw",
    "Если не закончим эту задачу, то наши друзья уйдут на ‘светлую сторону’. 🤯 #StarWars",
    "Не бойся этой задачи, она не кусается... сильно. 😏 #HarryPotter",
    "Эта задача – это как дух из лампы. Он не исчезнет, пока вы его не решите. 🧞‍♂️ #Aladdin",
    "Если вы сможете закончить эту задачу, мы все вознесем вас до уровня Дэдпула. 🦸‍♂️ #Deadpool",
    "Напомню, что награда за выполнение этой задачи - Кольцо Всевластия. 😎 #TheLordOfTheRings",
    "Ты знаешь, что должен сделать, так что давай закончим эту задачу и вернемся к работе. 🤷‍♀️ #TheMandalorian",
    "Ты готов исполнить свой долг, как Джон Сноу, правда? ❄️ #GameOfThrones",
    "Так, у нас есть эта задача, и у нас есть куча кофеина. Что может пойти не так? ☕️ #Friends",
    "Не забудь, что эта задача не такая уж и сложная, как Интерстеллар. 🚀 #Interstellar",
    "Если мы не сможем закончить эту задачу, на нас протестируют Аваду Кедавру. 🧙‍♀️ #HarryPotter",
    "Вот задача, которая станет нашим Рататуем. Нам нужно показать всем, что мы можем. 🐭 #Ratatouille",
    "Эта задача – наш Железный Человек. Мы можем сделать это! 💪 #IronMan",
    "Если мы не закончим эту задачу, мы все станем как Элвис Пресли – ушедшими из здания. 🕺 #MenInBlack",
    "Эта задача – как глубокий океан. Мы должны плыть, пока не доберемся до берега. 🌊 #FindingNemo",
    "Ну что, ты готов сделать эту задачу лучше, чем Локи? 😈 #Loki",
    "Ты знаешь, что говорят: с большой задачей нужно справляться постепенно. Но это не наш метод. 😏 #TheOffice",
    "Пришло время вернуть эту задачу обратно в 1985 год, чтобы она больше никогда не появлялась 🚀 #BackToTheFuture",
    "Давай закончим эту задачу и будем жить, как Джек из ‘Титаника’. Я о том моменте, когда он стоит на передней части корабля и кричит ‘Я король мира!’. 🚢 #Titanic",
    "Закончи эту задачу, и ты будешь чувствовать себя как Джон Сноу – знающий, но всё еще в беде. 😅 #GameOfThrones",
    "Эта задача легче, чем поднять молот Тора. ⚡️ #Avengers",
    "Напоминаю, что JIRA не может сделать эту задачу за тебя, так что не жди чуда. 🧙‍♂️ #HarryPotter",
    "Если мы не закончим эту задачу, тогда мы будем, как Дэрил из The Walking Dead - бродить без цели. 🧟‍♂️ #TheWalkingDead",
    "Эта задача проще, чем играть в Антуана Додсона в «Человека-невидимку». 👻 #Halloween",
    "Кажется, что эта задача постоянно переосмысливается, как личность Харви Дента в «Темном Рыцаре». 🦇 #Batman",
    "Сегодня закончим эту задачу, и мы сможем прославиться, как Ариэль из «Русалочки». 🧜‍♀️ #TheLittleMermaid",
    "Эта задача проще, чем технический аудит у Эллиота из Mr. Robot. 🤖 #MrRobot",
    "Надеюсь, ты не пытаешься обмануть систему, как Джек Спарро в «Пиратах Карибского моря». 🏴‍☠️ #PiratesOfTheCaribbean",
    "Закончим эту задачу, и мы сможем забыть про нее, как Марв и Гарри забыли про Кевина в «Один дома». 🏠 #HomeAlone",
    "Если мы не закончим эту задачу, тогда мы будем, как дети в «Стране Оз». 🌈 #TheWizardOfOz",
    "Давай закончим эту задачу, и мы будем чувствовать себя как Джеймс Бонд. 🕵️‍♂️ #JamesBond",
    "Если вы не можете закончить эту задачу, то лучше не спрашивать, как Дарт Вейдер на это отреагирует. 🤖 #StarWars",
    "Давайте закончим эту задачу как Майк Росс заканчивает дела на Suits. 💼 #Suits",
    "Если вы сделаете эту задачу, то ваш босс похож на Рейчел Зейн на первом свидании с Майком Россом. 🙄 #Suits",
    "Если мы закончим эту задачу, то мы сможем отдохнуть, как Эллери Куин после расследования дела. 📚 #TheHuntingParty",
    "Давайте закончим эту задачу так, чтобы у вас было больше эмоций, чем у Джо Рогана на пятничном шоу. 🤪 #TheJoeRoganExperience",
    "Не забывайте, что мы делаем это для себя и своей команды, а не для мистера Робота. 🤖 #MrRobot",
    "Закончим эту задачу, и тогда мы сможем отправиться в дальний космос 🚀 #Interstellar",
    "Если бы я был Генри Кавиллом, я бы завершил эту задачу уже давно. 😏 #TheWitcher",
    "Эта задача так же непроста, как и головоломка Шерлока Холмса 🔍 #SherlockHolmes",
    "Когда мы закончим эту задачу, я буду чувствовать себя как Ариель, выходящая из воды 🧜‍♀️ #TheLittleMermaid",
    "Завершим эту задачу и получим награду как Хан Соло получил свою награду 🏆 #StarWars",
    "Эта задача настолько сложна, что только Том Харди справится с ней. 😎 #MadMax"
  };

  public static String generateRandomReminder(
      String taskName, String issueKey, String issueLink, @Nullable String description) {
    Random rand = new Random();
    String template = REMINDER_TEMPLATES[rand.nextInt(REMINDER_TEMPLATES.length)];
    String additionalMessage = ADDITIONAL_MESSAGES[rand.nextInt(ADDITIONAL_MESSAGES.length)];

    StringBuilder msg = new StringBuilder("*🔔 Напоминание*\n");

    msg.append("\n").append(issueLink).append(" ").append("_").append(taskName).append("_");
    msg.append("\n\n").append(template.replace("[TASK_KEY]", issueKey));
    msg.append(
        (description != null && description.trim().length() > 0) ? "\n\n>" + description : "");
    msg.append("\n\n_").append(additionalMessage).append("_");

    return msg.toString();
  }
}
