package platform.analytics;

import com.google.gson.Gson;
import com.kaisquare.utils.LocalizableText;
import com.kaisquare.vca.thrift.TVcaAppInfo;
import lib.util.exceptions.ApiException;
import platform.analytics.app.AppVcaTypeMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class VcaAppInfo
{
    public final String appId;
    public final Program program;
    public final String version;
    public final LocalizableText displayName;
    public final LocalizableText description;

    public static VcaAppInfo fromThrift(TVcaAppInfo thriftObj) throws ApiException
    {
        Program program = Program.parse(thriftObj.getProgram());
        return new VcaAppInfo(thriftObj.getAppId(),
                              program,
                              thriftObj.getVersion(),
                              LocalizableText.create(thriftObj.getDisplayName()),
                              LocalizableText.create(thriftObj.getDescription()));
    }

    public static List<VcaAppInfo> fromKaiSync(List<Map> transportList) throws ApiException
    {
        Gson gson = new Gson();
        List<VcaAppInfo> parsedList = new ArrayList<>();
        for (Map map : transportList)
        {
            parsedList.add(new VcaAppInfo(String.valueOf(map.get("appId")),
                                          Program.parse(String.valueOf(map.get("program"))),
                                          String.valueOf(map.get("version")),
                                          gson.fromJson(gson.toJson(map.get("displayName")), LocalizableText.class),
                                          gson.fromJson(gson.toJson(map.get("description")), LocalizableText.class))
            );
        }
        return parsedList;
    }

    //old vca types
    public static List<VcaAppInfo> kaiX1Apps(String x1VcaVersion)
    {
        List<VcaAppInfo> x1Apps = new ArrayList<>();
        for (VcaType vcaType : VcaType.values())
        {
            String appId = AppVcaTypeMapper.getAppId(Program.KAI_X1, vcaType);
            LocalizableText displayText = LocalizableText.create(appId);
            LocalizableText descriptionText = LocalizableText.create(appId);
            switch (vcaType)
            {
                case AUDIENCE_PROFILING:
                    displayText = LocalizableText
                            .create("Audience Profiling")
                            .add("zh-cn", "用户分析")
                            .add("zh-tw", "用戶分析");
                    descriptionText = LocalizableText
                            .create("VCA to gather statistics about age/gender/emotions of the people in the monitoring area");
                    break;

                case TRAFFIC_FLOW:
                    displayText = LocalizableText.create("Human Traffic Flow")
                            .add("zh-cn", "客流量检测")
                            .add("zh-tw", "客流量檢測");
                    descriptionText = LocalizableText
                            .create("VCA to gather statistics about how people move around in the monitoring area");
                    break;

                case PEOPLE_COUNTING:
                    displayText = LocalizableText
                            .create("People Counting")
                            .add("zh-cn", "客流统计")
                            .add("zh-tw", "客流統計");
                    descriptionText = LocalizableText
                            .create("VCA to monitor IN/OUT/NET counts between two given regions.");
                    break;

                case CROWD_DETECTION:
                    displayText = LocalizableText
                            .create("Crowd Density")
                            .add("zh-cn", "客流密度")
                            .add("zh-tw", "客流密度");
                    descriptionText = LocalizableText
                            .create("VCA to gather crowd density information");
                    break;


                case AREA_INTRUSION:
                    displayText = LocalizableText
                            .create("Intrusion Detection")
                            .add("zh-cn", "区域入侵")
                            .add("zh-tw", "區域入侵");
                    descriptionText = LocalizableText
                            .create("VCA to detect area intrusions");
                    break;

                case PERIMETER_DEFENSE:
                    displayText = LocalizableText
                            .create("Perimeter Defense")
                            .add("zh-cn", "周界防御")
                            .add("zh-tw", "周界防禦");
                    descriptionText = LocalizableText
                            .create("VCA to detect breaches to perimeter defense lines");
                    break;

                case AREA_LOITERING:
                    displayText = LocalizableText
                            .create("Loitering Detection")
                            .add("zh-cn", "徘徊检测")
                            .add("zh-tw", "徘徊檢測");
                    descriptionText = LocalizableText
                            .create("VCA to detect persons loitering around the target area for too long");
                    break;

                case OBJECT_COUNTING:
                    displayText = LocalizableText
                            .create("Trip Wire Counting")
                            .add("zh-cn", "行线计数")
                            .add("zh-tw", "行線計數");
                    descriptionText = LocalizableText
                            .create("VCA to detect if the people count has exceeded the limit");
                    break;

                case VIDEO_BLUR:
                    displayText = LocalizableText
                            .create("Camera Tampering")
                            .add("zh-cn", "摄像机干扰")
                            .add("zh-tw", "攝像機幹擾");
                    descriptionText = LocalizableText
                            .create("VCA to detect camera tampering");
                    break;

                case FACE_INDEXING:
                    displayText = LocalizableText
                            .create("Face Indexing")
                            .add("zh-cn", "侦测脸部")
                            .add("zh-tw", "偵測臉部");
                    descriptionText = LocalizableText
                            .create("VCA to capture peoples' faces in the monitoring area");
                    break;
            }

            x1Apps.add(new VcaAppInfo(appId, Program.KAI_X1, x1VcaVersion, displayText, descriptionText));
        }
        return x1Apps;
    }

    private VcaAppInfo(String appId,
                       Program program,
                       String version,
                       LocalizableText displayName,
                       LocalizableText description)
    {
        this.appId = appId;
        this.program = program;
        this.version = version;
        this.displayName = displayName;
        this.description = description;
    }
}
